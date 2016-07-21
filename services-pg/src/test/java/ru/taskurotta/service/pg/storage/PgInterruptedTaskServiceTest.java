package ru.taskurotta.service.pg.storage;

import junit.framework.Assert;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.taskurotta.service.console.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class PgInterruptedTaskServiceTest {

    static String ACTOR_ID = "actor_id";
    static String STARTER_ID = "starter_id";
    static String ERR_CLASS = "java.lang.Exception";
    static String ERR_MESSAGE = "This is the error that causes troubles";

    static String FULL_MESSAGE = "This is the full message of the error that causes troubles";
    static String STACKTRACE = "This is the full stacktrace of the error that causes troubles";

    static long TIME = System.currentTimeMillis();

    JdbcTemplate jdbcTemplate;
    BasicDataSource ds;
    PgInterruptedTaskService target;

    @Before
    public void init() throws IOException {
//        File propsFile = new File(System.getProperty("pg.test.properties.location", "src/test/resources/pg.properties"));
        File propsFile = new File(System.getProperty("pg.test.properties.location", "pg.test.properties"));
        if (propsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                Properties props = new Properties();
                props.load(fis);
                ds = new BasicDataSource();
                ds.setDriverClassName("org.postgresql.Driver");
                ds.setUrl(props.getProperty("pg.db.url"));
                ds.setInitialSize(1);
                ds.setMaxActive(3);
                ds.setUsername(props.getProperty("pg.db.user"));
                ds.setPassword(props.getProperty("pg.db.password"));
                target = new PgInterruptedTaskService();
                target.setDataSource(ds);
                jdbcTemplate = new JdbcTemplate(ds);
                jdbcTemplate.update("delete from TSK_INTERRUPTED_TASKS");
            }
        }
    }

    @Test
    public void testSaveGetDelete() {
        UUID taskId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();
        target.save(createItd(processId, taskId), FULL_MESSAGE, STACKTRACE);

        Collection<InterruptedTask> tasks = target.findAll();
        Assert.assertTrue(tasks.size() == 1);
        InterruptedTask task = tasks.iterator().next();

        Assert.assertEquals(task.getProcessId(), processId);
        Assert.assertEquals(task.getTaskId(), taskId);
        Assert.assertEquals(task.getErrorMessage(), ERR_MESSAGE);
        Assert.assertEquals(task.getErrorClassName(), ERR_CLASS);
        Assert.assertEquals(task.getActorId(), ACTOR_ID);
        Assert.assertEquals(task.getStarterId(), STARTER_ID);
        Assert.assertEquals(task.getTime(), TIME);

        target.delete(processId, taskId);
        tasks = target.findAll();
        Assert.assertTrue(tasks.size() == 0);
    }

    @Test
    public void testMultipleSaves() {
        UUID taskId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();

        target.save(createItd(processId, taskId), FULL_MESSAGE, STACKTRACE);
        Assert.assertEquals(FULL_MESSAGE, target.getFullMessage(processId, taskId));
        Assert.assertEquals(STACKTRACE, target.getStackTrace(processId, taskId));

        target.save(createItd(processId, taskId), FULL_MESSAGE+"v2", STACKTRACE+"v2");
        Assert.assertEquals(FULL_MESSAGE+"v2", target.getFullMessage(processId, taskId));
        Assert.assertEquals(STACKTRACE+"v2", target.getStackTrace(processId, taskId));
    }

    @Test
    public void testFind() {
        int limit = 10;
        List<TaskIdentifier> ids = createTasks(limit);

        SearchCommand cmd = new SearchCommand();
        cmd.setActorId(ACTOR_ID);
        cmd.setStarterId(STARTER_ID);
        cmd.setStartPeriod(System.currentTimeMillis() - 60000l);
        cmd.setEndPeriod(System.currentTimeMillis() + 60000l);
        cmd.setErrorMessage(ERR_MESSAGE);
        cmd.setErrorClassName(ERR_CLASS);

        Collection<InterruptedTask> found = target.find(cmd);
        Assert.assertEquals(limit, found.size());

        TaskIdentifier first = ids.get(0);
        cmd.setProcessId(UUID.fromString(first.getProcessId()));
        cmd.setTaskId(null);
        found = target.find(cmd);
        Assert.assertEquals(1, found.size());

        cmd.setProcessId(null);
        cmd.setTaskId(UUID.fromString(first.getTaskId()));
        found = target.find(cmd);
        Assert.assertEquals(1, found.size());

        cmd.setProcessId(null);
        cmd.setTaskId(null);
        cmd.setErrorMessage(ACTOR_ID);
        found = target.find(cmd);
        Assert.assertEquals(0, found.size());
    }

    @Test
    public void testGroups() {
        String TEST_ACTOR1 = "test1";
        String TEST_ACTOR2 = "test2";
        int limit = 10;
        List<TaskIdentifier> ids1 = createTasks(limit);

        InterruptedTask task1 = createItd(UUID.randomUUID(), UUID.randomUUID());
        task1.setActorId(TEST_ACTOR1);
        target.save(task1, FULL_MESSAGE, STACKTRACE);

        InterruptedTask task2 = createItd(UUID.randomUUID(), UUID.randomUUID());
        task2.setActorId(TEST_ACTOR2);
        target.save(task2, FULL_MESSAGE, STACKTRACE);

        GroupCommand cmd = new GroupCommand();
        cmd.setGroup(GroupCommand.GROUP_ACTOR);
        List<TasksGroupVO> groups = target.getGroupList(cmd);
        Assert.assertEquals(3, groups.size());
        for (TasksGroupVO group : groups) {
            if (ACTOR_ID.equals(group.getName())) {
                Assert.assertEquals(limit, group.getTotal());
                Assert.assertEquals(1, group.getActorsCount());
                Assert.assertEquals(1, group.getStartersCount());
                Assert.assertEquals(1, group.getExceptionsCount());
            } else if (TEST_ACTOR1.equals(group.getName())) {
                Assert.assertEquals(1, group.getTotal());
                Assert.assertEquals(1, group.getActorsCount());
                Assert.assertEquals(1, group.getStartersCount());
                Assert.assertEquals(1, group.getExceptionsCount());

            } else if(TEST_ACTOR2.equals(group.getName())) {
                Assert.assertEquals(1, group.getTotal());
                Assert.assertEquals(1, group.getActorsCount());
                Assert.assertEquals(1, group.getStartersCount());
                Assert.assertEquals(1, group.getExceptionsCount());
            } else {
                Assert.assertTrue("Should never have group["+group.getName()+"]", Boolean.FALSE);
            }
        }

        cmd.setActorId(ACTOR_ID);
        Collection<TaskIdentifier> ids2 = target.getTaskIdentifiers(cmd);
        Assert.assertEquals(ids1.size(), ids2.size());
        for (TaskIdentifier tid : ids2) {
            Assert.assertTrue(ids1.contains(tid));
        }

        Set<UUID> processes =  target.getProcessIds(cmd);
        Assert.assertEquals(limit, processes.size());

        for (UUID pid : processes) {
            Assert.assertEquals(1, target.deleteTasksForProcess(pid));
        }

        processes =  target.getProcessIds(cmd);
        Assert.assertNull(processes);

    }

    List<TaskIdentifier> createTasks(int limit) {
        List<TaskIdentifier> ids = new ArrayList<>(limit);
        for (int i = 0; i<limit; i++) {
            UUID processId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            TaskIdentifier identifier = new TaskIdentifier(taskId, processId);
            ids.add(identifier);
            target.save(createItd(processId, taskId), FULL_MESSAGE, STACKTRACE);
        }
        return ids;
    }

    InterruptedTask createItd(UUID processId, UUID taskId) {
        InterruptedTask result = new InterruptedTask();
        result.setProcessId(processId);
        result.setTaskId(taskId);
        result.setActorId(ACTOR_ID);
        result.setStarterId(STARTER_ID);
        result.setTime(TIME);
        result.setErrorClassName(ERR_CLASS);
        result.setErrorMessage(ERR_MESSAGE);

        return result;
    }

//    long deleteTasksForProcess(UUID processId);


}

package ru.taskurotta.service.pg.storage;

import com.hazelcast.core.Hazelcast;
import junit.framework.Assert;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.storage.IdempotencyKeyViolation;
import ru.taskurotta.transport.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.fail;

public class PgProcessServiceTest {

    static final String METHOD = "run";
    static final String ACTOR_ID = "test#1.0";
    static final String CUSTOM_ID = "test_custom_id";
    static final String TASK_LIST = "test_task_list";
    static final long START_TIME = System.currentTimeMillis();
    static String RETURN_VALUE_JSON = "\"R_VALUE\"";

    PgProcessService target;
    JdbcTemplate jdbcTemplate;
    BasicDataSource ds;

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
                target = new PgProcessService(Hazelcast.newHazelcastInstance(), ds);
                jdbcTemplate = new JdbcTemplate(ds);
                jdbcTemplate.update("delete from tsk_process");
            }
        }
    }

    @After
    public void close() throws SQLException {
        if (ds != null) {
            ds.close();
        }
    }

    @Test
    public void testStartFinishDeleteMinimal() {
        if (target != null) {
            UUID pid1 = UUID.randomUUID();
            target.startProcess(getMinimalContainer(pid1));
            Process p1 = target.getProcess(pid1);
            Assert.assertNotNull(p1);
            Assert.assertEquals(pid1, p1.getProcessId());
            Assert.assertEquals(pid1, p1.getStartTaskId());
            Assert.assertNull(p1.getCustomId());
            Assert.assertTrue(p1.getStartTime() >= START_TIME);
            Assert.assertEquals(0l, p1.getEndTime());
            Assert.assertEquals(Process.ACTIVE, p1.getState());
            Assert.assertNull(p1.getReturnValue());

            TaskContainer startTask = p1.getStartTask();
            Assert.assertNotNull(startTask);
            Assert.assertEquals(pid1, startTask.getProcessId());
            Assert.assertEquals(START_TIME, startTask.getStartTime());
            Assert.assertEquals(METHOD, startTask.getMethod());

            TaskContainer pStartTask = target.getStartTask(pid1);
            Assert.assertEquals(pStartTask, startTask);

            target.markProcessAsAborted(pid1);
            Assert.assertEquals(Process.ABORTED, target.getProcess(pid1).getState());

            target.markProcessAsBroken(pid1);
            Assert.assertEquals(Process.BROKEN, target.getProcess(pid1).getState());

            target.markProcessAsStarted(pid1);
            Assert.assertEquals(Process.ACTIVE, target.getProcess(pid1).getState());

            target.finishProcess(pid1, RETURN_VALUE_JSON);
            p1 = target.getProcess(pid1);
            Assert.assertEquals(RETURN_VALUE_JSON, p1.getReturnValue());
            Assert.assertEquals(Process.FINISHED, p1.getState());

            target.deleteProcess(pid1);
        }
    }

    @Test
    public void testFindIncomplete() throws InterruptedException, IOException {
        if (target != null) {
            int count = 5;
            int limit = 3;
            Set<UUID> uuids = Collections.emptySet();
            try {
                uuids = createProcesses(5, false);
                ResultSetCursor<UUID> rsc = target.findIncompleteProcesses(System.currentTimeMillis()+60000l, limit);
                validateResultSetCursor(rsc, limit, uuids, count);
                rsc.close();
            } finally {
                for (UUID uuid : uuids) {
                    target.deleteProcess(uuid);
                }
            }

        }
    }

    @Test
    public void testFindLost() throws IOException {
        if (target != null) {
            int count = 5;
            int limit = 3;
            Set<UUID> uuids = createProcesses(count, false);
            finishProcesses(uuids);

            ResultSetCursor<UUID> rsc = target.findLostProcesses(System.currentTimeMillis()+60000l, 10l, limit);
            validateResultSetCursor(rsc, limit, uuids, count);
            rsc.close();
        }
    }

    @Test
    public void testCreateGetFull() {
        if (target != null) {
            UUID pid2 = UUID.randomUUID();
            target.startProcess(getFullContainer(pid2));
            try {
                Process p2 = target.getProcess(pid2);
                Assert.assertNotNull(p2);

                Assert.assertEquals(pid2, p2.getProcessId());
                Assert.assertEquals(pid2, p2.getStartTaskId());
                Assert.assertEquals(CUSTOM_ID, p2.getCustomId());
                Assert.assertTrue(p2.getStartTime() >= START_TIME);
                Assert.assertEquals(0l, p2.getEndTime());
                Assert.assertEquals(Process.ACTIVE, p2.getState());
                Assert.assertNull(p2.getReturnValue());

                TaskContainer startTask2 = p2.getStartTask();
                Assert.assertNotNull(startTask2);
                Assert.assertEquals(pid2, startTask2.getProcessId());
                Assert.assertEquals(START_TIME, startTask2.getStartTime());
                Assert.assertEquals(METHOD, startTask2.getMethod());
            } finally {
                target.deleteProcess(pid2);
            }
        }
    }

    @Test
    public void testRetrieverCounters() {
        if (target != null) {
            int count = 5;
            Set<UUID> uuids = Collections.emptySet();
            Set<UUID> uuidsFull = Collections.emptySet();
            try {
                uuids = createProcesses(count, false);
                uuidsFull = createProcesses(count, true);

                Assert.assertEquals(count, target.getActiveCount(ACTOR_ID, null));
                Assert.assertEquals(count, target.getActiveCount(ACTOR_ID, TASK_LIST));

                finishProcesses(uuids);
                Assert.assertEquals(count, target.getFinishedCount(null));

                finishProcesses(uuidsFull);

                for (UUID id : uuids) {
                    target.markProcessAsBroken(id);
                }
                Assert.assertEquals(count, target.getBrokenProcessCount());
            } finally {
                Set<UUID> processesSet = new HashSet<>();
                processesSet.addAll(uuids);
                processesSet.addAll(uuidsFull);
                for (UUID uuid : processesSet) {
                    target.deleteProcess(uuid);
                }
            }

        }
    }

    @Test
    public void testFindProcesses() {
        if (target != null) {
            int count = 5;
            int limit = 3;
            Set<UUID> uuids = Collections.emptySet();
            try {
                uuids = createProcesses(count, false);

                ProcessSearchCommand command = new ProcessSearchCommand();
                command.setActorId(ACTOR_ID);
                command.setStartedFrom(System.currentTimeMillis() - 60000l);
                command.setStartedTill(System.currentTimeMillis() + 60000l);
                command.setState(Process.ACTIVE);

                command.setPageNum(1);
                command.setPageSize(limit);

                GenericPage<Process> page = target.findProcesses(command);
                Assert.assertEquals(count, page.getTotalCount());
                Assert.assertEquals(limit, page.getItems().size());

                for (Process p : page.getItems()) {
                    Assert.assertTrue(uuids.contains(p.getProcessId()));
                }

                command.setPageNum(2);
                page = target.findProcesses(command);
                Assert.assertEquals(count-limit, page.getItems().size());
                Assert.assertEquals(count, page.getTotalCount());

                command.setPageNum(1);
                command.setProcessId(UUID.randomUUID().toString());
                page = target.findProcesses(command);
                Assert.assertEquals(0, page.getTotalCount());

                command.setProcessId(null);
                command.setCustomId(CUSTOM_ID);
                page = target.findProcesses(command);
                Assert.assertEquals(0, page.getTotalCount());

                target.startProcess(getFullContainer(UUID.randomUUID()));
                page = target.findProcesses(command);
                Assert.assertEquals(1, page.getTotalCount());
                Assert.assertEquals(1, page.getItems().size());
            } finally {
                for (UUID uuid : uuids) {
                    target.deleteProcess(uuid);
                }
            }
        }
    }

    @Test
    public void idempotencyTest() {
        if (target != null) {
            UUID pid = UUID.randomUUID();
            String idempotencyKey = UUID.randomUUID().toString();
            TaskContainer fullContainer = getFullContainer(pid);
            fullContainer.getOptions().getTaskConfigContainer().setIdempotencyKey(idempotencyKey);
            try {
                target.startProcess(fullContainer);
                target.startProcess(fullContainer);
                fail("no idempotency violation");
            } catch (IdempotencyKeyViolation ex) {
            } finally {
                target.finishProcess(pid, RETURN_VALUE_JSON);
                target.deleteProcess(pid);
            }
        }
    }

    void finishProcesses(Set<UUID> uuids) {
        for (UUID id : uuids) {
            target.finishProcess(id, "null");
        }
    }

    Set<UUID> createProcesses(int count, boolean isFullContainer) {
        Set<UUID> uuids = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            UUID pid = UUID.randomUUID();
            uuids.add(pid);
            target.startProcess(isFullContainer? getFullContainer(pid) : getMinimalContainer(pid));
        }
        return uuids;
    }

    void validateResultSetCursor(ResultSetCursor<UUID> rsc, int limit, Set<UUID> uuids, int count) {
        Collection<UUID> uuidsPack = rsc.getNext();
        Assert.assertEquals(limit, uuidsPack.size());

        for (UUID id : uuidsPack) {
            Assert.assertTrue(uuids.contains(id));
            uuids.remove(id);
            target.deleteProcess(id);
        }

        uuidsPack = rsc.getNext();
        Assert.assertEquals(count-limit, uuidsPack.size());

        for (UUID id : uuidsPack) {
            Assert.assertTrue(uuids.contains(id));
            uuids.remove(id);
            target.deleteProcess(id);
        }
    }

    TaskContainer getFullContainer(UUID processId) {
        TaskConfigContainer cfgContainer = new TaskConfigContainer(CUSTOM_ID, START_TIME, TASK_LIST, randomIdempotencyKey(), null, 1000000l);
        ArgType[] argTypes = new ArgType[1];
        argTypes[0] = ArgType.WAIT;
        TaskOptionsContainer options = new TaskOptionsContainer(argTypes, cfgContainer, null);

        String[] failTypes = new String[1];
        failTypes[0] = "java.lang.Throwable";
        ArgContainer[] args = new ArgContainer[1];
        args[0] = new ArgContainer("java.lang.String", ArgContainer.ValueType.PLAIN, UUID.randomUUID(), true, false, "\"test json string\"");
        return new TaskContainer(processId, processId, UUID.randomUUID(), METHOD, ACTOR_ID, TaskType.DECIDER_START,
                START_TIME, 5, args, options, false, failTypes);
    }

    TaskContainer getMinimalContainer(UUID processId) {
        return new TaskContainer(processId, processId, UUID.randomUUID(), METHOD, ACTOR_ID, TaskType.DECIDER_START, START_TIME, 0, null, null, false, null);
    }

    private static String randomIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

}

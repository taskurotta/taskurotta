package ru.taskurotta.service.pg.storage;

import junit.framework.Assert;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.schedule.JobConstants;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

public class PgSchedulerJobStoreTest {
    static final String METHOD = "run";
    static final String ACTOR_ID = "test#1.0";
    static final String CUSTOM_ID = "test_custom_id";
    static final String TASK_LIST = "test_task_list";
    static final String IDEMPOTENCE_KEY = "test_idempotence_key";
    static final long START_TIME = System.currentTimeMillis();
    static String RETURN_VALUE_JSON = "\"R_VALUE\"";

    private String TEST_JOB_NAME = "test job";
    private String TEST_JOB_CRON = "0/10 * * * * * ?";
    private int TEST_ERRR_CNT = 5;
    private int TEST_MAX_ERR = 10;
    private int TEST_LIMIT = 3;
    private String TEST_LAST_ERROR = "test last error message";

    JdbcTemplate jdbcTemplate;
    BasicDataSource ds;
    PgSchedulerJobStore target;

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
                target = new PgSchedulerJobStore();
                target.setDataSource(ds);
                jdbcTemplate = new JdbcTemplate(ds);
                jdbcTemplate.update("delete from TSK_SCHEDULED");
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
    public void test() {
        if (target != null) {
            JobVO original = instance(UUID.randomUUID());
            long id = target.add(original);
            Assert.assertTrue(id > 0);
            original.setId(id);
            JobVO stored = target.get(id);
            compare(original, stored);

            Collection<JobVO> col = target.getAll();
            Assert.assertEquals(1, col.size());

            compare(original, col.iterator().next());

            Collection<Long> keys = target.getKeys();
            Assert.assertEquals(1, col.size());
            Assert.assertEquals(Long.valueOf(id), keys.iterator().next());

            int status = target.getJobStatus(id);
            Assert.assertEquals(JobConstants.STATUS_ACTIVE, status);

            target.updateJobStatus(id, JobConstants.STATUS_INACTIVE);
            Assert.assertEquals(JobConstants.STATUS_INACTIVE, target.getJobStatus(id));

            target.updateErrorCount(id, TEST_ERRR_CNT+1, "test2");

            JobVO updated = target.get(id);
            Assert.assertEquals(TEST_ERRR_CNT+1, updated.getErrorCount());
            Assert.assertEquals("test2", updated.getLastError());


            updated.setCron("new cron");
            target.update(updated, id);

            JobVO updated2 = target.get(id);
            Assert.assertEquals("new cron", updated2.getCron());

        }
    }

    void compare(JobVO original, JobVO stored) {
        Assert.assertEquals(original.getId(), stored.getId());
        Assert.assertEquals(original.getName(), stored.getName());
        Assert.assertEquals(original.getCron(), stored.getCron());
        Assert.assertEquals(original.getTask(), stored.getTask());
        Assert.assertEquals(original.getStatus(), stored.getStatus());
        Assert.assertEquals(original.getErrorCount(), stored.getErrorCount());
        Assert.assertEquals(original.getLastError(), stored.getLastError());
        Assert.assertEquals(original.getMaxErrors(), stored.getMaxErrors());
        Assert.assertEquals(original.getLimit(), stored.getLimit());
    }

    JobVO instance(UUID processId) {
        JobVO result = new JobVO();
        result.setName(TEST_JOB_NAME);
        result.setCron(TEST_JOB_CRON);
        result.setTask(getFullContainer(processId));
        result.setStatus(JobConstants.STATUS_ACTIVE);
        result.setErrorCount(TEST_ERRR_CNT);
        result.setLastError(TEST_LAST_ERROR);
        result.setMaxErrors(TEST_MAX_ERR);
        result.setLimit(TEST_LIMIT);

        return result;
    }

    TaskContainer getFullContainer(UUID processId) {
        TaskConfigContainer cfgContainer = new TaskConfigContainer(CUSTOM_ID, START_TIME, TASK_LIST, IDEMPOTENCE_KEY, null, 1000000l);
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

}

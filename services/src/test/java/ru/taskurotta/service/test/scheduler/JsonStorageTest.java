package ru.taskurotta.service.test.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.schedule.JobConstants;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.service.schedule.storage.JsonDirectoryJobStore;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.internal.core.TaskType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Date: 18.12.13 11:46
 */
public class JsonStorageTest {

    private static final Logger logger = LoggerFactory.getLogger(JsonStorageTest.class);

    protected JsonDirectoryJobStore store;

    protected ObjectMapper mapper = new ObjectMapper();

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void prepareStoreDir() {
        store = new JsonDirectoryJobStore();
        store.setStoreLocation(tmpFolder.getRoot().getPath());
        store.init();
    }

    @Test
    public void testAddAndRemoveJob() throws Exception {

        int countOfTestJobsAtStore = 20;
        for (int i = 0; i<countOfTestJobsAtStore; i++) {
            store.addJob(getNewJob());
        }

        Collection<Long> ids = store.getJobIds();
        Set<Long> uniqueIds = new HashSet<>(ids);
        Assert.assertTrue(uniqueIds.size() == countOfTestJobsAtStore);

        for (Long id: ids) {
            Assert.assertTrue(id>0);//id != 0, cause it leads to UI problems AND id<0 means that job creation failed
        }

        for (Long id: ids) {
            store.removeJob(id);
        }

        Collection<Long> newIds = store.getJobIds();
        Assert.assertTrue(newIds==null || newIds.isEmpty());

        JobVO aJob = getNewJob();
        long id = store.addJob(aJob);
        logger.debug("New stored job id is [{}]", id);

        aJob.setId(id);
        String aJobAsString = mapper.writeValueAsString(aJob);

        JobVO theJob = store.getJob(id);
        Assert.assertNotNull(theJob);
        Assert.assertTrue(theJob.getId() == id);

        String theJobAsString = mapper.writeValueAsString(theJob);

        Assert.assertEquals(theJobAsString, aJobAsString);

        store.removeJob(id);
        theJob = store.getJob(id);
        logger.debug("Job after removal is [{}]", theJob);
        Assert.assertNull(theJob);

    }

    @Test
    public void testJobUpdates() {
        JobVO aJob = getNewJob();
        aJob.setStatus(JobConstants.STATUS_INACTIVE);

        logger.debug("STORED job is [{}]", aJob);
        long id = store.addJob(aJob);
        aJob.setId(id);

        store.updateJobStatus(id, JobConstants.STATUS_ACTIVE);
        JobVO theJob = store.getJob(id);
        logger.debug("LOADED job is [{}]", aJob);
        Assert.assertTrue(theJob.getStatus() == JobConstants.STATUS_ACTIVE);

        int status = store.getJobStatus(id);
        Assert.assertTrue(status == JobConstants.STATUS_ACTIVE);

        String newMethodName = "newMethodName";
        TaskContainer task = aJob.getTask();
        TaskContainer newTask = new TaskContainer(task.getTaskId(), task.getProcessId(), newMethodName, task.getActorId(), task.getType(), task.getStartTime(), task.getNumberOfAttempts(), task.getArgs(), task.getOptions(), task.isUnsafe(), task.getFailTypes());
        aJob.setTask(newTask);
        aJob.setStatus(JobConstants.STATUS_INACTIVE);
        store.updateJob(aJob);
        theJob = store.getJob(id);

        Assert.assertTrue(theJob.getTask().getMethod().equals(newMethodName));
        Assert.assertTrue(theJob.getStatus() == JobConstants.STATUS_INACTIVE);

        for (int i = 0; i<5; i++) {
            store.updateErrorCount(id, i, "Error #" + i);
        }
        theJob = store.getJob(id);

        Assert.assertTrue(theJob.getErrorCount() == 4);
        Assert.assertTrue(theJob.getLastError().equals("Error #" + 4));

    }

    protected JobVO getNewJob() {
        JobVO result = new JobVO();
        result.setName("Test job");
        result.setCron("0/5 * * * * ?");
        result.setTask(getNewTaskContainer());

        logger.debug("JOB created is [{}]", result);
        return result;
    }

    protected TaskContainer getNewTaskContainer() {
        return new TaskContainer(UUID.randomUUID(), UUID.randomUUID(), "doThis", "actorId#2.4", TaskType.DECIDER_START, -1l, 10, null, null, false, null);
    }

}

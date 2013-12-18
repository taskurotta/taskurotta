package ru.taskurotta.schedule.test.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.storage.file.JsonDirectoryJobStore;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskType;

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

//    public Collection<Long> getJobIds();
//
//    public void updateJobStatus(long id, int status);
//
//    public void updateJob(JobVO jobVO);
//
//    public int getJobStatus(long jobId);
//
//    public void updateErrorCount(long jobId, int count, String message);


    @Test
    public void testAddAndRemoveJob() throws Exception {
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



    protected JobVO getNewJob() {
        JobVO result = new JobVO();
        result.setName("Test job");
        result.setCron("0/5 * * * * ?");
        result.setTask(getNewTaskContainer());
        return result;
    }

    protected TaskContainer getNewTaskContainer() {
        return new TaskContainer(UUID.randomUUID(), UUID.randomUUID(), "doThis", "actorId#2.4", TaskType.DECIDER_START, -1l, 10, null, null, null);
    }

}

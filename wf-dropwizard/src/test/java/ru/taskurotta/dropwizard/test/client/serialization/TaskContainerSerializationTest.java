package ru.taskurotta.dropwizard.test.client.serialization;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.dropwizard.client.serialization.wrapper.TaskContainerWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskContainerSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(TaskContainerSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testTaskContainerDeserialization() {
        ObjectMapper jacksonMapper = new ObjectMapper();

        TaskContainer original = EntitiesFactory.createTaskContainer();

        TaskContainerWrapper resultWrapper = null;
        try {
            File tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, new TaskContainerWrapper(original));
            resultWrapper = jacksonMapper.readValue(tmpJsonFile, TaskContainerWrapper.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of TaskContainerWrapper to tmp File", e);
            Assert.fail("Exception at (de)serialization of TaskContainerWrapper to tmp File");
        }

        Assert.assertNotNull(resultWrapper);

        if(resultWrapper != null) {
            TaskContainer result = resultWrapper.getTaskContainer();
            logger.debug("TaskContainer getted is[{}]", result);
            Assert.assertNotNull(result);

            if(result!=null) {
                Assert.assertEquals("Task UUIDs must be the same", original.getTaskId(), result.getTaskId());
                Assert.assertEquals("Start times must be the same", original.getStartTime(), result.getStartTime());
                Assert.assertEquals("Number of attempts must be the same", original.getNumberOfAttempts(), result.getNumberOfAttempts());

                //validateTaskTarget(original.getTarget(), result.getTarget());
                Assert.assertEquals("Task methods must be the same", original.getMethod(), result.getMethod());
                Assert.assertEquals("Task actorIds must be the same", original.getActorId(), result.getActorId());
                Assert.assertEquals("Task types must be the same", original.getType(), result.getType());
                validateTaskArgs(original.getArgs(), result.getArgs());
                EntitiesComparator.compare(original.getOptions(), result.getOptions());

            }
        }

    }

    private void validateTaskArgs(ArgContainer[] original, ArgContainer[] validating) {
        if(original!=null) {
            Assert.assertNotNull(validating);
            if(validating != null) {
                Assert.assertEquals("Args array size must be the same", original.length, validating.length);
                if(original.length == validating.length) {
                    for(int i = 0;i<validating.length;i++) {
                        EntitiesComparator.compare(original[i], validating[i]);
                    }
                }
            }
        }
    }

}

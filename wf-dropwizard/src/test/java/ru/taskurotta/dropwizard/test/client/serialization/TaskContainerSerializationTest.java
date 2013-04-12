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

        EntitiesComparator.compare(original, resultWrapper.getTaskContainer());
    }

}

package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.File;

public class TaskContainerSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(TaskContainerSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testTaskContainerDeserialization() {
        ObjectMapper jacksonMapper = new ObjectMapper();

        TaskContainer original = EntitiesFactory.createTaskContainer();

        TaskContainer result = null;
        try {
            File tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, original);
            result = jacksonMapper.readValue(tmpJsonFile, TaskContainer.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of TaskContainerWrapper to tmp File", e);
            Assert.fail("Exception at (de)serialization of TaskContainerWrapper to tmp File");
        }

        Assert.assertNotNull(result);

        EntitiesComparator.compare(original, result);
    }

}

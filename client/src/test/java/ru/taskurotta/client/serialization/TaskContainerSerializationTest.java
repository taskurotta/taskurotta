package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TaskContainerSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(TaskContainerSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testTaskContainerDeserialization() throws IOException {
        ObjectMapper jacksonMapper = new ObjectMapper();

        TaskContainer original = EntitiesFactory.createTaskContainer();

        TaskContainer result = null;
        File tmpJsonFile = null;
        BufferedReader reader = null;
        String fileContent = null;
        try {
            tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, original);
            reader = new BufferedReader(new FileReader(tmpJsonFile));
            fileContent = reader.readLine();
            result = jacksonMapper.readValue(tmpJsonFile, TaskContainer.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of TaskContainer to tmp File", e);
            Assert.fail("Exception at (de)serialization of TaskContainer to tmp File");
        } finally {
            logger.error("File content: \n" + fileContent);
            if (reader!=null) {
                reader.close();
            }
        }

        Assert.assertNotNull(result);

        EntitiesComparator.compare(original, result);
    }

}

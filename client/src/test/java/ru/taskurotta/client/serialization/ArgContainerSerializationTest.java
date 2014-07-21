package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.ArgContainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

/**
 * Created on 28.04.2014.
 */
public class ArgContainerSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(ArgContainerSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testTaskContainerDeserialization() throws IOException {
        ObjectMapper jacksonMapper = new ObjectMapper();
        UUID uuid = UUID.randomUUID();

        ArgContainer original = EntitiesFactory.createArgPojoValue(uuid);

        ArgContainer result = null;
        File tmpJsonFile = null;
        String fileContent = null;
        BufferedReader reader = null;
        try {
            tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, original);
            reader = new BufferedReader(new FileReader(tmpJsonFile));
            fileContent = reader.readLine();
            result = jacksonMapper.readValue(tmpJsonFile, ArgContainer.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of ArgContainer to tmp File", e);
            Assert.fail("Exception at (de)serialization of ArgContainer to tmp File");
        } finally {
            logger.debug("FileContent: \n" + fileContent);
            if (reader!=null) {
                reader.close();
            }
        }

        Assert.assertNotNull(result);

        EntitiesComparator.compare(original, result);
    }


}

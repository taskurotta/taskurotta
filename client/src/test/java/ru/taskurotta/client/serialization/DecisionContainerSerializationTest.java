package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.DecisionContainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DecisionContainerSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(DecisionContainerSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testDecisionContainerSerialization() throws IOException {
        ObjectMapper jacksonMapper = new ObjectMapper();

        DecisionContainer original = EntitiesFactory.createDecisionContainer(false);

        DecisionContainer result = null;
        File tmpJsonFile = null;
        String fileContent = null;
        BufferedReader reader = null;
        try {
            tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, original);
            reader = new BufferedReader(new FileReader(tmpJsonFile));
            fileContent = reader.readLine();
            result = jacksonMapper.readValue(tmpJsonFile, DecisionContainer.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of DecisionContainer to tmp File", e);
            Assert.fail("Exception at (de)serialization of DecisionContainer to tmp File");
        } finally {
            logger.debug("FileContent: \n" + fileContent);
            if (reader!=null) {
                reader.close();
            }
        }

        Assert.assertNotNull(result);
        EntitiesComparator.compare(original, result);

    }

    @Test
    public void testErrorDecisionContainerSerialization() {
        ObjectMapper jacksonMapper = new ObjectMapper();

        DecisionContainer original = EntitiesFactory.createDecisionContainer(true);

        DecisionContainer result = null;
        try {
            File tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, original);
            result = jacksonMapper.readValue(tmpJsonFile, DecisionContainer.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of DecisionContainerWrapper to tmp File", e);
            Assert.fail("Exception at (de)serialization of DecisionContainerWrapper to tmp File");
        }

        Assert.assertNotNull(result);
        EntitiesComparator.compare(original, result);
    }

}

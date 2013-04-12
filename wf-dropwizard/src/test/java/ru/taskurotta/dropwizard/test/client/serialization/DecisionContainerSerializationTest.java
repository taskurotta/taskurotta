package ru.taskurotta.dropwizard.test.client.serialization;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.dropwizard.client.serialization.wrapper.DecisionContainerWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DecisionContainerSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(DecisionContainerSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testDecisionContainerSerialization() {
        ObjectMapper jacksonMapper = new ObjectMapper();

        DecisionContainer original = EntitiesFactory.createDecisionContainer(false);

        DecisionContainerWrapper resultWrapper = null;
        try {
            File tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, new DecisionContainerWrapper(original));
            resultWrapper = jacksonMapper.readValue(tmpJsonFile, DecisionContainerWrapper.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of DecisionContainerWrapper to tmp File", e);
            Assert.fail("Exception at (de)serialization of DecisionContainerWrapper to tmp File");
        }

        Assert.assertNotNull(resultWrapper);
        EntitiesComparator.compare(original, resultWrapper.getResultContainer());

    }

    @Test
    public void testErrorDecisionContainerSerialization() {
        ObjectMapper jacksonMapper = new ObjectMapper();

        DecisionContainer original = EntitiesFactory.createDecisionContainer(true);

        DecisionContainerWrapper resultWrapper = null;
        try {
            File tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, new DecisionContainerWrapper(original));
            resultWrapper = jacksonMapper.readValue(tmpJsonFile, DecisionContainerWrapper.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of DecisionContainerWrapper to tmp File", e);
            Assert.fail("Exception at (de)serialization of DecisionContainerWrapper to tmp File");
        }

        Assert.assertNotNull(resultWrapper);
        EntitiesComparator.compare(original, resultWrapper.getResultContainer());
    }

}

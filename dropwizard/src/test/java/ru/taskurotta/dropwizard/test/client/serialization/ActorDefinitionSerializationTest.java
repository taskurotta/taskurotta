package ru.taskurotta.dropwizard.test.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.util.ActorDefinition;

import java.io.File;

public class ActorDefinitionSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(ActorDefinitionSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testActorDefinitionSerialization() {
        ActorDefinition original = EntitiesFactory.createActorDefinition();

        ObjectMapper jacksonMapper = new ObjectMapper();

        ActorDefinition result = null;
        try {
            File tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, original);
            result = jacksonMapper.readValue(tmpJsonFile, ActorDefinition.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of ActorDefinition to tmp File", e);
            Assert.fail("Exception at (de)serialization of ActorDefinition to tmp File");
        }

        Assert.assertNotNull(result);
        if (result != null) {
            Assert.assertEquals("ActorDefinition names must be equal", original.getName(), result.getName());
            Assert.assertEquals("ActorDefinition versions must be equal", original.getVersion(), result.getVersion());
        }

    }

}

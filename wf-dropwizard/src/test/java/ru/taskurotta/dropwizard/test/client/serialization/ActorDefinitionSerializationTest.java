package ru.taskurotta.dropwizard.test.client.serialization;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.dropwizard.client.serialization.wrapper.ActorDefinitionWrapper;
import ru.taskurotta.util.ActorDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ActorDefinitionSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(ActorDefinitionSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testActorDefinitionSerialization() {
        ActorDefinition original = createActorDefinition();

        ObjectMapper jacksonMapper = new ObjectMapper();

        ActorDefinitionWrapper resultWrapper = null;
        try {
            File tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, new ActorDefinitionWrapper(original));
            resultWrapper = jacksonMapper.readValue(tmpJsonFile, ActorDefinitionWrapper.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of ActorDefinitionWrapper to tmp File", e);
            Assert.fail("Exception at (de)serialization of ActorDefinitionWrapper to tmp File");
        }

        Assert.assertNotNull(resultWrapper);
        if(resultWrapper != null) {
            ActorDefinition validating = resultWrapper.getActorDefinition();
            Assert.assertNotNull(validating);
            if(validating!=null) {
                Assert.assertEquals("ActorDefinition names must be equal", original.getName(), validating.getName());
                Assert.assertEquals("ActorDefinition versions must be equal", original.getVersion(), validating.getVersion());
            }
        }

    }

    private ActorDefinition createActorDefinition() {
        return ActorDefinition.valueOf("test.me.worker", "7.6.5");
    }

}

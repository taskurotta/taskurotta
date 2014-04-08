package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.jersey.serialization.TransportJacksonModule;
import ru.taskurotta.util.ActorDefinition;

import java.io.StringWriter;

public class ActorDefinitionSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(ActorDefinitionSerializationTest.class);

    @Test
    public void testActorDefinitionSerialization() {
        innerTestActorDefinitionSerialization(ActorDefinition.valueOf("test.me.worker", "7.6.5"));
        innerTestActorDefinitionSerialization(ActorDefinition.valueOf("test.me.worker", "7.6.5", "testTaskList"));
    }

    public void innerTestActorDefinitionSerialization(ActorDefinition original) {

        ObjectMapper jacksonMapper = new ObjectMapper();
        jacksonMapper.registerModule(new TransportJacksonModule());

        ActorDefinition result = null;
        try {
            String json = jacksonMapper.writer().writeValueAsString(original);
            System.err.println("JSON = " + json);
            result = jacksonMapper.readValue(json, ActorDefinition.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of ActorDefinition to String", e);
            Assert.fail("Exception at (de)serialization of ActorDefinition to String");
        }

        Assert.assertNotNull(result);
        if (result != null) {
            Assert.assertEquals("ActorDefinition names must be equal", original.getName(), result.getName());
            Assert.assertEquals("ActorDefinition versions must be equal", original.getVersion(), result.getVersion());
            Assert.assertEquals("ActorDefinition taskLists must be equal", original.getTaskList(),
                    result.getTaskList());
            Assert.assertEquals("ActorDefinition fullName must be equal", original.getFullName(),
                    result.getFullName());
        }

    }

}

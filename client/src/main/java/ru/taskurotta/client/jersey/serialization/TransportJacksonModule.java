package ru.taskurotta.client.jersey.serialization;

import com.fasterxml.jackson.databind.module.SimpleModule;
import ru.taskurotta.util.ActorDefinition;

/**
 */
public class TransportJacksonModule extends SimpleModule {

    public TransportJacksonModule() {
        addDeserializer(ActorDefinition.class, new ActorDefinitionDeserializer());
        addSerializer(ActorDefinition.class, new ActorDefinitionSerializer());
    }

}

package ru.taskurotta.client.jersey.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import ru.taskurotta.util.ActorDefinition;

import java.io.IOException;

public class ActorDefinitionSerializer extends JsonSerializer<ActorDefinition> implements Constants {

    @Override
    public void serialize(ActorDefinition actorDefinition, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField(ACTOR_DEFINITION_NAME, actorDefinition.getName());
        jgen.writeStringField(ACTOR_DEFINITION_VERSION, actorDefinition.getVersion());

        String taskList = actorDefinition.getTaskList();
        if (taskList != null) {
            jgen.writeStringField(ACTOR_DEFINITION_TASK_LIST, actorDefinition.getTaskList());
        }

        jgen.writeEndObject();
    }

}

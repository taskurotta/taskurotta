package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import ru.taskurotta.util.ActorDefinition;

import java.io.IOException;

public class ActorDefinitionSerializer extends JsonSerializer<ActorDefinition>{

	@Override
	public void serialize(ActorDefinition actorDefinition, JsonGenerator jgen, SerializerProvider provider) throws IOException,
			JsonProcessingException {
		jgen.writeStartObject();
		jgen.writeStringField("name", actorDefinition.getName());
		jgen.writeStringField("version", actorDefinition.getVersion());
		jgen.writeEndObject();
	}

}

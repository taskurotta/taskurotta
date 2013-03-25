package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import ru.taskurotta.util.ActorDefinition;

import java.io.IOException;

public class ActorDefinitionDeserializer extends JsonDeserializer<ActorDefinition> implements Constants{

	@Override
	public ActorDefinition deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		ObjectCodec oc = jp.getCodec();
		JsonNode rootNode = oc.readTree(jp);
		
		String name = rootNode.get(ACTOR_DEFINITION_NAME).textValue();
		String version = rootNode.get(ACTOR_DEFINITION_VERSION).textValue();
		
		return ActorDefinition.valueOf(name, version);
	}
	
}

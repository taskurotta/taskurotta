package ru.taskurotta.dropwizard.client.serialization;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.storage.model.TaskContainer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class TaskContainerDeserializer extends JsonDeserializer<TaskContainer> implements Constants {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskContainerDeserializer.class);
	
	@Override
	public TaskContainer deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ObjectCodec oc = jp.getCodec();
		JsonNode rootNode = oc.readTree(jp);
		
		return DeserializationHelper.parseTaskContainer(rootNode);
		
	}
	
}

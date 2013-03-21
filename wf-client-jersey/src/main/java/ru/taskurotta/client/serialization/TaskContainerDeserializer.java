package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.server.transport.ArgContainer;
import ru.taskurotta.server.transport.TaskContainer;

import java.io.IOException;
import java.util.UUID;

public class TaskContainerDeserializer extends JsonDeserializer<TaskContainer> {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskContainerDeserializer.class);
	
	@Override
	public TaskContainer deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ObjectCodec oc = jp.getCodec();
		JsonNode rootNode = oc.readTree(jp);
				
		logger.debug("Deserializing Task from JSON[{}]", rootNode.asText());
		
		UUID taskId = DeserializationHelper.extractId(rootNode.get("taskId"), null);
		TaskTarget target = DeserializationHelper.extractTaskTarget(rootNode.get("target"), null);
		ArgContainer[] args = DeserializationHelper.extractArgs(rootNode.get("args"), null);
		
		return new TaskContainer(taskId, target, args);
	}
	
}

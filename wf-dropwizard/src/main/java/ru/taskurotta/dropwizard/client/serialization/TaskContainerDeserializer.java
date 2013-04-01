package ru.taskurotta.dropwizard.client.serialization;

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
import ru.taskurotta.server.transport.TaskOptionsContainer;

import java.io.IOException;
import java.util.UUID;

public class TaskContainerDeserializer extends JsonDeserializer<TaskContainer> implements Constants {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskContainerDeserializer.class);
	
	@Override
	public TaskContainer deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ObjectCodec oc = jp.getCodec();
		JsonNode rootNode = oc.readTree(jp);
				
		logger.debug("Deserializing Task from JSON[{}]", rootNode);
		
		UUID taskId = DeserializationHelper.extractId(rootNode.get(TASK_ID), null);
		TaskTarget target = DeserializationHelper.extractTaskTarget(rootNode.get(TASK_TARGET), null);
		ArgContainer[] args = DeserializationHelper.extractArgs(rootNode.get(TASK_ARGS), null);
		TaskOptionsContainer options = DeserializationHelper.extractOptions(rootNode.get(TASK_OPTIONS), null);

        //TODO: deserialize startTime and numberOfAttempt

		return new TaskContainer(taskId, target, 0, 1, args, options);
	}
	
}

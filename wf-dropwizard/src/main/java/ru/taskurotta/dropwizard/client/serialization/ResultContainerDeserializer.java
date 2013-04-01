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
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.ErrorContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.server.transport.TaskOptionsContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ResultContainerDeserializer extends JsonDeserializer<DecisionContainer> implements Constants {
	
	private static final Logger logger = LoggerFactory.getLogger(ResultContainerDeserializer.class);
	
	@Override
	public DecisionContainer deserialize(JsonParser jp,
			DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		
		ObjectCodec oc = jp.getCodec();
		JsonNode rootNode = oc.readTree(jp);
				
		logger.debug("Deserializing Task from JSON[{}]", rootNode);

		UUID taskId = DeserializationHelper.extractId(rootNode.get(RESULT_TASK_ID), null);
		ArgContainer value = DeserializationHelper.parseArgument(rootNode.get(RESULT_VALUE));
		Boolean error = rootNode.get(RESULT_IS_ERROR).booleanValue();
		ErrorContainer errorContainer = null;
		TaskContainer[] tasks = null;
		
		JsonNode tasksNode = rootNode.get(RESULT_TASKS);
		if(tasksNode!=null && !tasksNode.isNull() && tasksNode.isArray()) {
			List<TaskContainer> taskList = new ArrayList<TaskContainer>();
			
			Iterator<JsonNode> argsIterator = tasksNode.elements();
			while(argsIterator.hasNext()) {
				taskList.add(parseTaskContainer(argsIterator.next()));
			}
			tasks = taskList.toArray(new TaskContainer[taskList.size()]);
			
		}
		
		return new DecisionContainer(taskId, value, error, errorContainer, tasks);
	}

    // TODO: reuse TaskContainerDeserializer logic
	private TaskContainer parseTaskContainer(JsonNode tcNode) {
		UUID taskId = DeserializationHelper.extractId(tcNode.get(TASK_ID), null);
		TaskTarget target = DeserializationHelper.extractTaskTarget(tcNode.get(TASK_TARGET), null);
		ArgContainer[] args = DeserializationHelper.extractArgs(tcNode.get(TASK_ARGS), null);
        TaskOptionsContainer options = DeserializationHelper.extractOptions(tcNode.get(TASK_OPTIONS), null);

        //TODO: deserialize startTime, numberOfAttempt


		return new TaskContainer(taskId, target, 0, 0, args, options);
	}

}

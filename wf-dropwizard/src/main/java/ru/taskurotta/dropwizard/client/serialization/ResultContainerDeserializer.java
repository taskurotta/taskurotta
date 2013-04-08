package ru.taskurotta.dropwizard.client.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

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
        UUID processId = DeserializationHelper.extractId(rootNode.get(RESULT_PROCESS_ID), null);
		ArgContainer value = DeserializationHelper.parseArgument(rootNode.get(RESULT_VALUE));
		Boolean error = rootNode.get(RESULT_IS_ERROR).booleanValue();
		ErrorContainer errorContainer = null;
		TaskContainer[] tasks = null;
		
		JsonNode tasksNode = rootNode.get(RESULT_TASKS);
		if(tasksNode!=null && !tasksNode.isNull() && tasksNode.isArray()) {
			List<TaskContainer> taskList = new ArrayList<TaskContainer>();
			
			Iterator<JsonNode> argsIterator = tasksNode.elements();
			while(argsIterator.hasNext()) {
				taskList.add(DeserializationHelper.parseTaskContainer(argsIterator.next()));
			}
			tasks = taskList.toArray(new TaskContainer[taskList.size()]);
			
		}
		
		return new DecisionContainer(taskId, processId, value, error, errorContainer, tasks);
	}

}

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
import ru.taskurotta.server.transport.ErrorContainer;
import ru.taskurotta.server.transport.ResultContainer;
import ru.taskurotta.server.transport.TaskContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ResultContainerDeserializer extends JsonDeserializer<ResultContainer> {
	
	private static final Logger logger = LoggerFactory.getLogger(ResultContainerDeserializer.class);
	
	@Override
	public ResultContainer deserialize(JsonParser jp,
			DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		
		ObjectCodec oc = jp.getCodec();
		JsonNode rootNode = oc.readTree(jp);
				
		logger.debug("Deserializing Task from JSON[{}]", rootNode);

		UUID taskId = DeserializationHelper.extractId(rootNode.get("taskId"), null);
		ArgContainer value = DeserializationHelper.parseArgument(rootNode.get("value"));
		Boolean error = rootNode.get("error").booleanValue();
		ErrorContainer errorContainer = null;
		TaskContainer[] tasks = null;
		
		JsonNode tasksNode = rootNode.get("tasks");
		if(tasksNode!=null && !tasksNode.isNull() && tasksNode.isArray()) {
			List<TaskContainer> taskList = new ArrayList<TaskContainer>();
			
			Iterator<JsonNode> argsIterator = tasksNode.elements();
			while(argsIterator.hasNext()) {
				taskList.add(parseTaskContainer(argsIterator.next()));
			}
			tasks = taskList.toArray(new TaskContainer[taskList.size()]);
			
		}
		
		return new ResultContainer(taskId, value, error, errorContainer, tasks);
	}
	
	private TaskContainer parseTaskContainer(JsonNode tcNode) {
		UUID taskId = DeserializationHelper.extractId(tcNode.get("taskId"), null);
		TaskTarget target = DeserializationHelper.extractTaskTarget(tcNode.get("target"), null);
		ArgContainer[] args = DeserializationHelper.extractArgs(tcNode.get("args"), null);
		
		return new TaskContainer(taskId, target, args);		
	}

}

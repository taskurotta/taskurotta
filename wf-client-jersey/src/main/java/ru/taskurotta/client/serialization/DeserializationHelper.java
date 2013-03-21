package ru.taskurotta.client.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.server.transport.ArgContainer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class DeserializationHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(DeserializationHelper.class);
	
	public static Task extractTask(JsonNode taskNode, Task defVal) {
		UUID uuid = DeserializationHelper.extractId(taskNode.get("id"), null);
		TaskTarget taskTarget = DeserializationHelper.extractTaskTarget(taskNode.get("target"), null);
		Object[] args = DeserializationHelper.extractArgs(taskNode.get("args"), null);
				
		return new TaskImpl(uuid, taskTarget, args);
	} 
	
	public static UUID extractId(JsonNode idNode, UUID defVal) {
		UUID result = defVal;
		if(idNode!=null && !idNode.isNull()) {
			result = UUID.fromString(idNode.textValue());
		} else {
			logger.debug("Cannot extract UUID from node [{}]", idNode);
		}
		return result;
	}
	
	public static TaskTarget extractTaskTarget(JsonNode targetNode, TaskTarget defVal) {
		TaskTarget result = defVal;
		if(targetNode!=null && !targetNode.isNull()) {

			String taskType = targetNode.get("type").textValue();
			TaskType tasktypeEnumVal = TaskType.valueOf(taskType);
			String taskMethod = targetNode.get("method").textValue();
			String taskName = targetNode.get("name").textValue();
			String taskVersion = targetNode.get("version").textValue();
			
			result = new TaskTargetImpl(tasktypeEnumVal, taskName, taskVersion, taskMethod);
		} else {
			logger.debug("Cannot extract TaskTarget from node [{}]", targetNode);
		}
		return result;
	}
	
	public static ArgContainer[] extractArgs(JsonNode argsNode, ArgContainer[] defVal) {
		ArgContainer[] result = defVal;
		if(argsNode!=null && !argsNode.isNull() && argsNode.isArray()) {
			Iterator<JsonNode> argsIterator = argsNode.elements();
			List<ArgContainer> argumentsList = new ArrayList<ArgContainer>();
			while(argsIterator.hasNext()) {
				JsonNode arg = argsIterator.next();
				
				argumentsList.add(parseArgument(arg));
			}
			result = argumentsList.toArray(new ArgContainer[argumentsList.size()]);
		} else {
			logger.debug("Cannot extract task args from node[{}]", argsNode);
		}
		
		return result;
	}
	
	public static ArgContainer parseArgument(JsonNode arg) {
		ArgContainer result = null;
		if(arg==null || arg.isNull()) {
			return result;
		} else {
			String className = arg.get("className").textValue();
			Boolean isPromise = arg.get("promise").booleanValue();
			UUID taskId = extractId(arg.get("taskId"), null);
			Boolean isReady = arg.get("ready").booleanValue();
			String json = arg.get("jsonvalue").textValue();
			
			return new ArgContainer(className, isPromise, taskId, isReady, json);
		}
	}
		
}

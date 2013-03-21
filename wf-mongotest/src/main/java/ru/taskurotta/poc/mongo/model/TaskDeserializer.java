package ru.taskurotta.poc.mongo.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * created by void 04.02.13 14:00
 */
public class TaskDeserializer extends JsonDeserializer<Task> {

	@Override
	public Task deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		Task task = new Task(node.get("_id").asText(),
				node.get("taskId").asInt(),
				node.get("processed").asBoolean());

		for (JsonNode tag : node.get("tags")) {
			task.setTag(tag.asText());
		}

		return task;
	}
}

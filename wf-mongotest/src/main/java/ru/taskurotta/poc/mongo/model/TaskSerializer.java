package ru.taskurotta.poc.mongo.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * created by void 04.02.13 13:43
 */
public class TaskSerializer extends JsonSerializer<Task> {

	@Override
	public void serialize(Task task, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
		jsonGenerator.writeStartObject();
		jsonGenerator.writeStringField("_id", task.id);

		jsonGenerator.writeNumberField("taskId", task.taskId);
		jsonGenerator.writeBooleanField("processed", task.processed);

		jsonGenerator.writeArrayFieldStart("tags");
		for (String tag : task.tag) {
			jsonGenerator.writeString(tag);
		}
		jsonGenerator.writeEndArray();

		jsonGenerator.writeEndObject();
	}
}

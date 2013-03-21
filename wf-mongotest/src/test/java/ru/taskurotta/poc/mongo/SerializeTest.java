package ru.taskurotta.poc.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.taskurotta.poc.mongo.model.Task;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * created by void 05.02.13 14:20
 */
public class SerializeTest {
	private ObjectMapper mapper;
	private static final String DATA = "{\"_id\":\"id-123\",\"taskId\":1,\"processed\":false,\"tags\":[\"tag1\",\"tag2\"]}";

	public SerializeTest() {
		mapper = new ObjectMapper();
	}

	@Test
	public void serializeTest() throws JsonProcessingException {
		Task task = new Task("id-123", 1, false);
		task.setTag("tag1");
		task.setTag("tag2");

		String str = mapper.writeValueAsString(task);
		System.out.println(str);
		assertEquals(DATA, str);
	}

	@Test
	public void deserializeTest() throws IOException {

		Task task = mapper.readValue(DATA, Task.class);
		assertNotNull(task);
	}
}

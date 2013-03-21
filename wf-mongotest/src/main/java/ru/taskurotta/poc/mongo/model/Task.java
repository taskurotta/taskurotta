package ru.taskurotta.poc.mongo.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;

import java.util.ArrayList;
import java.util.List;

/**
* created by void 29.01.13 13:58
*/
@JsonDeserialize(using = TaskDeserializer.class)
@JsonSerialize(using = TaskSerializer.class)
public class Task {
	@Id
	public String id;
	public Integer taskId;
	public boolean processed;
	public List<String> tag;

	public Task() {
	}

	@PersistenceConstructor
	public Task(String id, Integer taskId, boolean processed) {
		this.id = id;
		this.taskId = taskId;
		this.processed = processed;
		this.tag = new ArrayList<String>();
	}

	public Task(Integer taskId, String tag) {
		this.tag = new ArrayList<String>();

		this.taskId = taskId;
		setTag("c");
		setTag(tag);
	}

	public Task setTag(String tag) {
		this.tag.add(tag);
		return this;
	}

	/**
	 *
	 */
	public static class Parameter {
		private String key;
		private String value;

		public Parameter() {
		}

		public Parameter(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}

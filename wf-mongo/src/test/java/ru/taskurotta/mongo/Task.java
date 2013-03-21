package ru.taskurotta.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;

import java.util.ArrayList;
import java.util.List;

/**
* created by void 29.01.13 13:58
*/
class Task {
	@Id
	public String id;
	public Integer taskId;
	public boolean processed;
	public List<String> tag;

	Task() {
	}

	@PersistenceConstructor
	Task(String id, Integer taskId, boolean processed) {
		this.id = id;
		this.taskId = taskId;
		this.processed = processed;
	}

	Task(Integer taskId, String tag) {
		this.tag = new ArrayList<String>();

		this.taskId = taskId;
		setTag("c");
		setTag(tag);
	}

	public Task setTag(String tag) {
		this.tag.add(tag);
		return this;
	}
}

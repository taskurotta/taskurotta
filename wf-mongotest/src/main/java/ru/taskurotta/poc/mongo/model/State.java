package ru.taskurotta.poc.mongo.model;

import java.util.Date;

/**
 * created by void 12.02.13 11:39
 */
public class State {
	private Date time;
	private String value;
	private String actor;

	public State(String value, String actor) {
		this(new Date(), value, actor);
	}

	public State(Date time, String value, String actor) {
		this.time = time;
		this.value = value;
		this.actor = actor;
	}

	public Date getTime() {
		return time;
	}

	public String getValue() {
		return value;
	}

	public String getActor() {
		return actor;
	}

}

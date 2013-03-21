package ru.taskurotta.poc.mongo.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import ru.taskurotta.poc.mongo.model.State;

/**
 * created by void 12.02.13 11:42
 */
public class DBObjectFactory {

	public static DBObject fromState(State state) {
		return new BasicDBObject()
				.append("time", state.getTime())
				.append("value", state.getValue())
				.append("actor", state.getActor());
	}
}

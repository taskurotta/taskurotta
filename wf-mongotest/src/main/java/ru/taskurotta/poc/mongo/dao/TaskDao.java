package ru.taskurotta.poc.mongo.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import com.mongodb.util.JSON;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import ru.taskurotta.poc.mongo.model.State;
import ru.taskurotta.poc.mongo.model.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * created by void 01.01.13 15:52
 */
public class TaskDao {
	private static final String taskCollection = "task";
	private static final String logCollection = "deciderLog";
	private MongoTemplate template;

	public void setDB(Mongo mongo, String dbName) {
		template = new MongoTemplate(mongo, dbName);
		template.setWriteConcern(WriteConcern.NORMAL);
	}

	public void saveLog(long start, long end, long count) {
		HashMap<String, Long> map = new HashMap<String, Long>(3);
		map.put("start", start);
		map.put("end", end);
		map.put("count", count);
		template.save(map, logCollection);
	}

	public List<Map> getLog() {
		return template.findAll(Map.class, logCollection);
	}

	public void reCreateCollection() {
		template.getCollection(logCollection).drop();
		template.createCollection(logCollection);
		template.getCollection(taskCollection).drop();
		template.createCollection(taskCollection);
	}

	public void setupIndex(String... field) {
		BasicDBObject keys = new BasicDBObject();
		for (String key : field) {
			keys.append(key, 1);
		}
		template.getCollection(taskCollection).createIndex(keys);
	}

	public void save(Task task) {
		template.save(task, taskCollection);
	}

	public Task selectForProcess(int deciderId) {
		return template.findAndModify(new Query(Criteria.where("processed").is(false)),
				Update.update("processed", true), Task.class, taskCollection);
	}

	public long count() {
		DBCollection collection = template.getCollection(taskCollection);
		return collection.count();
	}

	public long countFree() {
		DBCollection collection = template.getCollection(taskCollection);  //executeCommand()
		BasicDBObject query = new BasicDBObject("processed", false);
		DBCursor cursor = collection.find(query);
		return cursor.count();
	}

	public DBCursor findAll() {
		DBCollection collection = template.getCollection(taskCollection);  //executeCommand()
		BasicDBObject query = new BasicDBObject("processed", false);
		return collection.find();
	}

	public void delete(DBObject next) {
		template.remove(Query.query(Criteria.where("_id").is(next.get("_id"))), taskCollection);
	}

	public void saveJson(String json) {
		template.save(JSON.parse(json), taskCollection);
	}

	public Map findTask(String oldState, State newState, long time, boolean sort) {
		Query query = Query.query(Criteria.where("promiseCount").is(0)
				.andOperator(Criteria.where("state.value").is(oldState)
				.andOperator(Criteria.where("state.time").lt(time)))
		);
		if (sort) {
			query = query.with(new Sort(new Sort.Order(Sort.Direction.ASC, "_id")));
		}

		DBObject state = DBObjectFactory.fromState(newState);
		Update update = Update.update("state", state);
		return template.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Map.class, taskCollection);
	}

	public Map findTask(String oldState, State newState, long time, String tag, boolean sort) {
		Query query = Query.query(Criteria.where("args").all(tag)
				.andOperator(Criteria.where("promiseCount").is(0)
				.andOperator(Criteria.where("state.value").is(oldState)
				.andOperator(Criteria.where("state.time").lt(time)
				)))
		);
		if (sort) {
			query = query.with(new Sort(new Sort.Order(Sort.Direction.ASC, "_id")));
		}

		DBObject state = DBObjectFactory.fromState(newState);
		Update update = Update.update("state", state);
		return template.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Map.class, taskCollection);
	}

	public Map findTask(State newState) {
		return findTask("wait", newState, 0, false);
	}

	public Update getUpdateState(State newState) {
		DBObject state = DBObjectFactory.fromState(newState);
		return Update.update("$set", new BasicDBObject("state", state))
				.addToSet("$push", new BasicDBObject("stateHistory", state));
	}

	public void updateStatus(String id, Update update) {
		Query query = Query.query(Criteria.where("uuid").is(id));
		template.updateFirst(query, update, taskCollection);
	}

	public void decrementPromiseCount(String[] waiting) {
		Query query = Query.query(Criteria.where("uuid").in(waiting));
		Update update = Update.update("$inc", "promiseCount");
		template.updateMulti(query, update, taskCollection);
	}

    public List find(Query query) {
        return template.find(query, BasicDBObject.class, taskCollection);
    }
}

package ru.taskurotta.poc.mongo.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import ru.taskurotta.poc.mongo.model.DeciderTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class DeciderTaskDao {
	private static final String taskCollection = "task";
	private static final String logCollection = "deciderLog";
	private MongoTemplate template;

	public void setDB(Mongo mongo, String dbName) {
		template = new MongoTemplate(mongo, dbName);
//		template.setWriteResultChecking(WriteResultChecking.EXCEPTION);
//		WriteConcern writeConcern = new WriteConcern(2, 10000, true, true, false);
//		template.setWriteConcern(writeConcern);
		template.setWriteConcern(WriteConcern.NORMAL);
	}

	public void save(DeciderTask task) {
		template.save(task, taskCollection);
	}

	public DeciderTask get(int id) {
		return template.findOne(new Query(Criteria.where("taskId").is(id)), DeciderTask.class, taskCollection);
	}

	public DeciderTask get(String _id) {
		return template.findOne(new Query(Criteria.where("_id").is(_id)), DeciderTask.class, taskCollection);
	}

	public long count() {
		DBCollection collection = template.getCollection(taskCollection);
		return collection.count();
	}

	public void delete(DeciderTask task) {
		template.remove(task);
	}

	public long countFree() {
		DBCollection collection = template.getCollection(taskCollection);  //executeCommand()
		BasicDBObject query = new BasicDBObject("deciderId", 0);
		DBCursor cursor = collection.find(query);
		return cursor.count();
	}

	public void clear() {
		template.dropCollection("deciderTask");
	}

	public void selectForDecider(int deciderId) {
		template.updateFirst(new Query(Criteria.where("deciderId").is(0)), Update.update("deciderId", deciderId), taskCollection);
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

}

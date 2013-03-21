package ru.taskurotta.poc.mongo.test;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import ru.taskurotta.poc.mongo.model.State;

import java.util.Date;
import java.util.Map;

/**
 * created by void 12.02.13 13:57
 */
public class LockTask extends DBTest {
	private static final String taskTemplate = "{" +
			"  uuid: \"#startTaskId\"," +
			"  state: {" +
			"    time: #statusTime," +
			"    value: \"wait\"," +
			"    actor: \"wf_starter\"" +
			"  }," +
			"  stateHistory: [" +
			"    { time: #statusTime, value: \"wait\", actor: \"wf_starter\" }" +
			"  ]," +
			"  target: {" +
			"    type: 1," +
			"    name: \"ru.taskurotta.apidesign.SimpleTestDecider\"," +
			"    version: \"1.0\"," +
			"    method: \"start\"" +
			"  }," +
			"  args: []," +
			"  result: {}," +
			"  waiting: []," +
			"  parent: []," +
			"  promiseCount: 0" +
			"}";

	private Date midTime;
	protected String arg = "one";

	@Override
	public void prepare(int total) {
		log.debug("prepare index: [\"promiseCount\", \"state.value\", \"state.time\", \"_id\"]");
		dao.reCreateCollection();
		dao.setupIndex("args", "promiseCount", "state.value", "state.time", "_id");
		createTasks(total*2, 1000, "'one', 'two'");

		DBCursor allTasks = dao.findAll();
		log.debug("task count: {}", allTasks.count());

		for (int i=0; i < total; i++) {
			if (i % 2 == 0) {
				allTasks.next();
				DBObject next = allTasks.next();
				dao.delete(next);
			} else {
				DBObject next = allTasks.next();
				dao.delete(next);
				allTasks.next();
			}
		}
		allTasks = dao.findAll();
		log.debug("task count: {}", allTasks.count());

		midTime = new Date(new Date().getTime() - 10);

		createTasks(total, 20000, "'two', 'three'");
		log.debug("task count: {}", allTasks.count());
/*
		allTasks = dao.findAll();
		while(allTasks.hasNext()) {
			DBObject task = allTasks.next();
			log.debug("found task state {}", task);
		}
//*/
	}

	private void createTasks(int total, int shift, String args) {
		for (int i=0; i<total; i++) {
			String startTask = taskTemplate.replace("#startTaskId", ""+(i+shift));
			startTask = startTask.replace("#statusTime", ""+ new Date().getTime());
			startTask = startTask.replace("args: []", "args: ["+ args +"]");
			dao.saveJson(startTask);
		}
	}

	@Override
	public void execute() {
		Date startTime = midTime;
		log.debug("start time {}", startTime.getTime());
		long lastId = -1;
		for (int i=0; i<count; i++) {
			State started = new State(new Date(), "started", "decider000-b0c5-6f513191d765");
			Map task = dao.findTask("wait", started, startTime.getTime(), arg, false);
			if (null != task) {
				String uuid = (String) task.get("uuid");
//			log.debug("found task id {}", uuid);
				long currentId = Long.parseLong(uuid);
				if (currentId < lastId) {
					log.error("Incorrect order {}: lastId: {}, current: {}", i, lastId, currentId);
				}
				lastId = currentId;
			}
//			log.debug("found task: {}", task);
		}
	}
}

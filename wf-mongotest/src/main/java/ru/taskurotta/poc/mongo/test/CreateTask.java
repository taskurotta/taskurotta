package ru.taskurotta.poc.mongo.test;

import com.mongodb.BasicDBObject;
import org.springframework.data.mongodb.core.query.Update;
import ru.taskurotta.poc.mongo.model.State;

import java.util.Map;
import java.util.UUID;

/**
 * created by void 11.02.13 16:05
 */
public class CreateTask extends DBTest {

	private static final String taskTemplate = "{" +
			"  uuid: \"#startTaskId\"," +
			"  state: {" +
			"    time: 1223434324," +
			"    value: \"wait\"," +
			"    actor: \"wf_starter\"" +
			"  }," +
			"  stateHistory: [" +
			"    { time: 1223434324, value: \"wait\", actor: \"wf_starter\" }" +
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

	@Override
	public void prepare(int total) {
		dao.reCreateCollection();
		dao.setupIndex("state.value", "promiseCount");
	}

	@Override
	public void execute() {
		/* 1 */
		String startTaskId = UUID.randomUUID().toString();
		String startTask = taskTemplate.replace("#startTaskId", startTaskId);
		dao.saveJson(startTask);

		/* 2 */
		String starterId = UUID.randomUUID().toString();
		dao.findTask(new State("started", starterId));

		/* 3 */
		String task1Id = UUID.randomUUID().toString();
		String task2Id = UUID.randomUUID().toString();
		String task3Id = UUID.randomUUID().toString();

		String task1 = taskTemplate.replace("#startTaskId", task1Id);
		task1 = task1.replace("waiting: [],", "waiting: ["+ task2Id +"],");
		task1 = task1.replace("parent: [],", "parent: ["+ startTaskId +"],");

		String task2 = taskTemplate.replace("#startTaskId", task2Id);
		task2 = task2.replace("args: [],", "args: [[Promise, { uuid: "+ task1Id +" }] ],");
		task2 = task2.replace("waiting: [],", "waiting: ["+ task3Id +"],");
		task2 = task2.replace("parent: [],", "parent: ["+ startTaskId +"],");
		task2 = task2.replace("promiseCount: 0", "promiseCount: 1");

		String task3 = taskTemplate.replace("#startTaskId", task3Id);
		task3 = task3.replace("args: [],", "args: [[Promise, { uuid: "+ task2Id +" }] ],");
		task3 = task3.replace("waiting: [],", "waiting: ["+ startTaskId +"],");
		task3 = task3.replace("parent: [],", "parent: ["+ startTaskId +"],");
		task3 = task3.replace("promiseCount: 0", "promiseCount: 1");

		dao.saveJson(task1);
		dao.saveJson(task2);
		dao.saveJson(task3);

		/* 4 */
		Update waitChild = dao.getUpdateState(new State("waitChild", starterId));
		waitChild.addToSet("$set", new BasicDBObject("result", "[Promise, { uuid: " + task3Id + "}]"))
				.addToSet("$set", new BasicDBObject("promiseCount", 1));
		dao.updateStatus(startTaskId, waitChild);

		String postProcessorId  = UUID.randomUUID().toString();

		for (int i=0; i<3; i++) {
			/* 5 */
			String workerId  = UUID.randomUUID().toString();
			Map workerTask = dao.findTask(new State("started", workerId));

			/* 6 */
			Update done = dao.getUpdateState(new State("done", workerId));
			done.addToSet("$set", new BasicDBObject("result", 55));
			dao.updateStatus((String)workerTask.get("uuid"), done);

			/* 7 */
			Map postProcessorTask = dao.findTask(new State("processing", postProcessorId));

			/* 8 */
			String[] waiting = (String[])postProcessorTask.get("waiting");
			dao.decrementPromiseCount(waiting);

			/* 9 */
			Update processed = dao.getUpdateState(new State("processed", workerId));
			dao.updateStatus((String)postProcessorTask.get("uuid"), processed);
		}

		/* 10 */
		Map postProcessorTask = dao.findTask("waitChild", new State("processing", postProcessorId),0, false);

		/* 11 */
		/* 12 */
		Update processed = dao.getUpdateState(new State("processed", postProcessorId));
		processed.addToSet("$set", new BasicDBObject("result", "[Promise, { uuid: " + task3Id + ", valueRef="+ UUID.randomUUID() +"}]"));
		dao.updateStatus(startTaskId, processed);
	}
}

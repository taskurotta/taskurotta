package ru.taskurotta.poc.mongo.test;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 22.02.13
 * Time: 13:33
 */
public class SimpleTask extends DBTest {
    private static final String taskTemplate = "{" +
            "  uuid: \"#startTaskId\"," +
            "  state: {" +
            "    time: #stateTime," +
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
        dao.setupIndex("state.value", "promiseCount", "state.time");
    }

    @Override
    public void execute() {
        long time = System.currentTimeMillis();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            String startTaskId = UUID.randomUUID().toString();
            String startTask = taskTemplate.replace("#startTaskId", startTaskId);
            startTask = startTask.replace("#stateTime", String.valueOf(time));
            dao.saveJson(startTask);
        }

        Query query = Query.query(Criteria.where("state.value").is("wait"));
        long startTime = System.currentTimeMillis();
        List tasks = dao.find(query);
        long endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("state.value").is("wait"));
        query.with(new Sort(Sort.Direction.ASC, "state.value"));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("state.value").is("wait").and("state.time").lt(startTime));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("state.value").is("wait").and("promiseCount").is(0));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("promiseCount").is(0).and("state.time").lt(startTime));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("promiseCount").is(0).and("state.time").lt(startTime));
        query.with(new Sort(Sort.Direction.DESC, "state.time"));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("state.value").is("wait").and("promiseCount").is(0).and("state.time").lt(startTime));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("state.value").is("wait").and("promiseCount").is(0).and("state.time").lt(startTime));
        query.with(new Sort(Sort.Direction.DESC, "state.time"));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("state.value").is("wait").and("state.time").lt(startTime).and("promiseCount").is(0));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);

        query = Query.query(Criteria.where("state.value").is("wait").and("state.time").lt(startTime).and("promiseCount").is(0));
        query.with(new Sort(Sort.Direction.DESC, "state.time"));
        startTime = System.currentTimeMillis();
        tasks = dao.find(query);
        endTime = System.currentTimeMillis();
        log.info("Found [{}] tasks for query [{}] [{}] milliseconds", query.toString(), tasks.size(), endTime - startTime);
    }
}

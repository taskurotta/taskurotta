package ru.taskurotta.poc.mongo.test;

import ru.taskurotta.poc.mongo.model.Task;

/**
 * created by void 01.02.13 13:22
 */
public class SimpleUpdateTask extends DBTest {

	@Override
	public void prepare(int total) {
		dao.reCreateCollection();
		dao.setupIndex("processed");

		for (int i=0; i<total; i++) {
			dao.save(new Task(i, "a"));
		}
	}

	@Override
	public void execute() {
		for (int i=0; i<count; i++) {
			dao.selectForProcess(id);
		}
	}
}

package ru.taskurotta.poc.mongo.test;

/**
 *
 */
public class AcquireTask extends DBTest {

	@Override
	public void prepare(int total) {
		dao.setupIndex("deciderId");
	}

	@Override
	public void execute() {
		for (int i=0; i<count; i++) {
			dao.selectForProcess(id);
		}
	}
}

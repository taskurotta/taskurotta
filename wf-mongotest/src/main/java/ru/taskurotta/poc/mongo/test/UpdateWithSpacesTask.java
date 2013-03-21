package ru.taskurotta.poc.mongo.test;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import ru.taskurotta.poc.mongo.model.Task;

/**
 * created by void 01.02.13 13:22
 */
public class UpdateWithSpacesTask extends DBTest {

	@Override
	public void prepare(int total) {
		dao.reCreateCollection();
		dao.setupIndex("processed");

		for (int i=0; i<total*2; i++) {
			dao.save(new Task(i, "a"));
		}

		int skipped = 0;
		for (int i=0; i<total*2; i++) {
			DBCursor all = dao.findAll();
			DBObject next = all.next();

			if (skipped > 100) {
				dao.delete(next);
			}
			skipped++;
			if (skipped > 200) {
				skipped = 0;
			}
		}
	}

	@Override
	public void execute() {
		for (int i=0; i<count; i++) {
			dao.selectForProcess(id);
		}
	}
}

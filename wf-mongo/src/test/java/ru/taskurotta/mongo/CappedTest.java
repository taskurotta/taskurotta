package ru.taskurotta.mongo;

import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.UnknownHostException;

/**
 * for database initialization use this comands in mongo shell:
 * use test
 * db.createCollection("run", {capped:true, size:1000000})
 * db.run.ensureIndex({tag:1})
 * db.run.ensureIndex({processed:1})
 *
 * created by void 29.01.13 13:00
 */
@Ignore
public class CappedTest {
	protected final static Logger log = LoggerFactory.getLogger(CappedTest.class);
	private static final String DATABASE_NAME = "test";
	private static final String COLLECTION = "run";
	private static final int THREAD_COUNT = 10;
	private static final int UPDATE_COUNT = 1000;
	private MongoOperations mops;
	private DBCollection collection;

	public CappedTest() throws UnknownHostException {
		Mongo mongo = new Mongo();
		mops = new MongoTemplate(mongo, DATABASE_NAME);
		collection = mops.getCollection(COLLECTION);
	}

	@Test
	public void populate() {
		long start = System.currentTimeMillis();
/*
		for (int i=0; i<100000; i++) {
			mops.insert(new Task(i, "a"), COLLECTION);
		}
*/
		for (int i=0; i<100000; i++) {
			mops.insert(new Task(i, "a"), COLLECTION);
			mops.insert(new Task(i, "b"), COLLECTION);
		}
		long end = System.currentTimeMillis();
		log.debug("test time: " + ((end - start) / 1000.0));
	}

	@Test
	public void accuire() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(THREAD_COUNT);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();

		for (int i=0; i < THREAD_COUNT; i++) {
			final String tag = i % 2 == 0 ? "a" : "b";

			executor.execute( new Runnable() {
				@Override
				public void run() {
					log.debug("started thread {}", Thread.currentThread().getName());
					long start = System.currentTimeMillis();

					//DBCursor cursor = collection.find(new BasicDBObject("processed", false).append("tag", tag));
					for (int i=0; i< UPDATE_COUNT; i++) {
						findNext(tag);
					}

					long end = System.currentTimeMillis();
					log.debug("updated: " + UPDATE_COUNT + " test time: " + ((end - start) / 1000.0));
				}
			});
		}

		executor.shutdown();

		try {
			Thread.sleep(1000);
			int wait_seconds = 120;
			while(executor.getActiveCount() > 0 && wait_seconds > 0) {
				Thread.sleep(1000);
				wait_seconds--;
				log.trace("active threads: {}", executor.getActiveCount());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	public Task findNext(String tag) {
		return mops.findAndModify(new Query(Criteria.where("processed").is(false).andOperator(Criteria.where("tag").is(tag))),
				Update.update("processed", true), Task.class, COLLECTION);
	}

}

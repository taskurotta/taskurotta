package ru.taskurotta.poc.mongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ru.taskurotta.poc.mongo.dao.TaskDao;
import ru.taskurotta.poc.mongo.test.DBTest;

import java.util.Map;

/**
 *
 */
public class Launcher {

	protected static final Logger log = LoggerFactory.getLogger(Launcher.class);

	private ThreadPoolTaskExecutor executor;
	@Autowired
	private TaskDao dao;

	public void launch(DBTest dbTest, int workerCount, int iterationCount) throws CloneNotSupportedException {
		log.info("before launch {} of {}, for {} iterations", workerCount, dbTest.getClass().getName(), iterationCount);

		dbTest.prepare(workerCount * iterationCount);
		for (int i=0; i < workerCount; i++) {
			DBTest worker = dbTest.getClone(i);
			executor.execute(worker);
		}
		executor.shutdown();
	}

	public void finish() {
		log.info("Task count {}, free tasks: {}", dao.count(), dao.countFree());

		long start=Long.MAX_VALUE, end=0, count=0;
		for (Map log: dao.getLog()) {
			long start1 = (Long)log.get("start");
			if (start > start1) {
				start = start1;
			}
			long end1 = (Long)log.get("end");
			if (end < end1) {
				end = end1;
			}
			long count1 = (Long)log.get("count");
			count += count1;
		}
		log.info("final rate: {}", ((double)count)/(end - start) * 1000.0);
	}

	public void setExecutor(ThreadPoolTaskExecutor executor) {
		this.executor = executor;
	}

	public void setDao(TaskDao dao) {
		this.dao = dao;
	}

	public static void main(String args[]) throws CloneNotSupportedException {
		log.info("start!");

		AbstractApplicationContext ctx = new AnnotationConfigApplicationContext(MongoConfiguration.class);
		ctx.registerShutdownHook();

		DBTest test = (DBTest)ctx.getBean(args[0]);

		int workerCount = Integer.parseInt(args[1]);
		int iterationCount = Integer.parseInt(args[2]);
		test.setCount(iterationCount);

		Launcher launcher = (Launcher)ctx.getBean("launcher");
		launcher.launch(test, workerCount, iterationCount);
	}
}

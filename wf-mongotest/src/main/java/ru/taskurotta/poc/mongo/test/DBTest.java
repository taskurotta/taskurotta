package ru.taskurotta.poc.mongo.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.taskurotta.poc.mongo.dao.TaskDao;

/**
 *
 */
public abstract class DBTest implements Runnable, Cloneable {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected int id;
	protected int count;

	@Autowired
	protected TaskDao dao;

	public abstract void prepare(int total);
	public abstract void execute();

	@Override
	public void run() {
		long start = System.currentTimeMillis();

		execute();

		long end = System.currentTimeMillis();
		dao.saveLog(start, end, count);

		log.info("updated {} records, rate {} rps", count, ((double) count) / (end - start) * 1000.0);
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setDao(TaskDao dao) {
		this.dao = dao;
	}

	public DBTest getClone(int id) throws CloneNotSupportedException {
		DBTest clone = (DBTest) clone();
		clone.setId(id);
		return clone;
	}

}

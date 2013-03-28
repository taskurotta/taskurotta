package ru.taskurotta.recipes.nowait.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by void 27.03.13 19:40
 */
public class FastWorkerImpl implements FastWorker {
	protected final static Logger log = LoggerFactory.getLogger(FastWorkerImpl.class);

	@Override
	public int taskB() {
		log.info("taskB()");
		return 1;
	}

	@Override
	public int taskC() {
		log.info("taskC()");
		return 2;
	}

	@Override
	public int taskE(int b) {
		log.info("taskE({})", b);
		return 3;
	}
}

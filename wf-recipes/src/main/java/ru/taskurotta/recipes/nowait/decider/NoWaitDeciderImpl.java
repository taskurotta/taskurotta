package ru.taskurotta.recipes.nowait.decider;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.NoWait;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.nowait.workers.FastWorkerClient;
import ru.taskurotta.test.FlowArbiter;

/**
 * Created by void 27.03.13 17:11
 */
public class NoWaitDeciderImpl implements NoWaitDecider {
	protected final static Logger log = LoggerFactory.getLogger(NoWaitDeciderImpl.class);

	private FlowArbiter arbiter;
	private FastWorkerClient worker;
	private NoWaitDeciderImpl async;

	@Override
	public void start() {
		log.info("start");
		arbiter.notify("start");
		Promise<Integer> pB = worker.taskB();
		Promise<Integer> pC = worker.taskC();
		async.process(pB, pC);
	}

	@Asynchronous
	public Promise<Integer> process(@NoWait Promise<Integer> b, Promise<Integer> c) {
		log.info("process({}, {})", b, c.get());
		arbiter.notify("process");
		return worker.taskE(b.get());
	}

	public void setWorker(FastWorkerClient worker) {
		this.worker = worker;
	}

	public void setAsync(NoWaitDeciderImpl async) {
		this.async = async;
	}

	public void setArbiter(FlowArbiter arbiter) {
		this.arbiter = arbiter;
	}
}

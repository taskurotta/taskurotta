package ru.taskurotta.recipes.summator.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.summator.ArbiterProfilerImpl;
import ru.taskurotta.recipes.summator.worker.AddWorkerClient;
import ru.taskurotta.test.FlowArbiterFactory;
import ru.taskurotta.test.IncorrectFlowException;

import java.util.List;

/**
 * Created by void 05.04.13 18:59
 */
public class SummatorDeciderImpl implements SummatorDecider {
    protected final static Logger log = LoggerFactory.getLogger(SummatorDeciderImpl.class);

	private AddWorkerClient worker;
	private SummatorDeciderImpl selfAsync;

	@Override
	@SuppressWarnings("unchecked")
	public void start(List<Integer> data) {
		if (data.size() == 0) {
			return;
		}
		if (data.size() == 1) {
            log.info("result: {}", data.get(0));
            return;
		}

		Promise<Integer>[] preResult = new Promise[(int)Math.ceil(data.size() / 2.0)];
		for (int i=0; i<preResult.length; i++) {
			if (2 * i + 1 < data.size()) {
				preResult[i] = worker.add(Promise.asPromise(data.get(2*i)), Promise.asPromise(data.get(2*i+1)));
			} else {
				preResult[i] = Promise.asPromise(data.get(2*i));
			}
		}

        selfAsync.waitForResult(add(preResult));
	}

	private Promise<Integer> add(Promise<Integer> data[]) {
		if (data.length == 1) {
			return data[0];
		}
		if (data.length == 2) {
			return worker.add(data[0], data[1]);
		}

		@SuppressWarnings("unchecked")
		Promise<Integer>[] preResult = new Promise[(int)Math.ceil(data.length / 2.0)];
		for (int i=0; i<preResult.length; i++) {
			if (2 * i + 1 < data.length) {
				preResult[i] = worker.add(data[2*i], data[2*i+1]);
			} else {
				preResult[i] = data[2*i];
			}
		}
		return add(preResult);
	}

    @Asynchronous
    public void waitForResult(Promise<Integer> result) {
        Integer firstResult = result.get();

        ArbiterProfilerImpl arbiter = (ArbiterProfilerImpl) new FlowArbiterFactory().getInstance(); // created in spring context
        int testResult = arbiter.getTestResult();

        log.info("result: {}; testResult: {}", firstResult, testResult);

        if (firstResult != testResult) {
            throw new IncorrectFlowException("TaskFlow result: "+ firstResult +" doesn't match test result: "+ testResult);
        }
    }

	public void setWorker(AddWorkerClient worker) {
		this.worker = worker;
	}

	public void setSelfAsync(SummatorDeciderImpl selfAsync) {
		this.selfAsync = selfAsync;
	}
}

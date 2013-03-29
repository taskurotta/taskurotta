package ru.taskurotta.recipes.nowait;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.test.FlowArbiter;
import ru.taskurotta.test.IncorrectFlowException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by void 29.03.13 11:18
 */
public class NoWaitArbiter implements FlowArbiter {
	protected final static Logger log = LoggerFactory.getLogger(NoWaitArbiter.class);

	private static final String[] tags = {"start", "taskC", "taskB", "process", "taskE"};
	private final List<String> stages;

	public NoWaitArbiter() {
		stages = new LinkedList<String>(Arrays.asList(tags));
	}

	@Override
	public void notify(String tag) {
		log.info("notified about tag [{}]; {}", tag, stages);

		synchronized (stages) {
			String current = stages.get(0);
			if (current.equals(tag)) {
				stages.remove(0);
			} else {
				throw new IncorrectFlowException("Wrong tag: expected ["+ current +"] but found ["+ tag +"]");
			}

			process(tag);
		}
	}

	private void process(String tag) {

		if ("taskB".equals(tag)) {
			try {
				String before = stages.get(0);
				stages.wait(10000);
				String after = stages.get(0);
				if (before.equals(after)) {
					throw new IncorrectFlowException("Task with @NoWait doesn't started");
				}
			} catch (InterruptedException e) {
				// ignore
			}
		}
		if ("process".equals(tag)) {
			stages.notifyAll();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// ignore
		}
	}
}

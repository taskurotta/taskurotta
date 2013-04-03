package ru.taskurotta.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by void 01.04.13 12:04
 */
public abstract class BasicFlowArbiter implements FlowArbiter {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final List<String> stages;

	private String lastTag;

	public BasicFlowArbiter(List<String> stages) {
		this.stages = stages;
	}

	@Override
	public void notify(String tag) {
		log.debug("notified about tag [{}]; {}", tag, stages);

		synchronized (stages) {
			String current = stages.get(0);
			if (current.equals(tag)) {
				lastTag = stages.remove(0);
			} else {
				throw new IncorrectFlowException("Wrong tag: expected ["+ current +"] but found ["+ tag +"]");
			}

			process(tag);
			stages.notifyAll();
		}

	}

	public String getCurrentStage() {
		return stages.get(0);
	}

	protected void waitForTag(String tag, long timeToWait) {
		try {
			long endTime = System.currentTimeMillis() + timeToWait;
			while (!lastTag.equals(tag) && timeToWait > 0) {
				stages.wait(timeToWait);
				timeToWait = endTime - System.currentTimeMillis();
			}
		} catch (InterruptedException e) {
			// just go away
		}
	}

	protected abstract void process(String tag);

	public boolean waitForFinish(long timeToWait) {
		long endTime = System.currentTimeMillis() + timeToWait;
		synchronized (stages) {
			try {
				while (stages.size() > 0 && timeToWait > 0) {

					stages.wait(timeToWait);
					timeToWait = endTime - System.currentTimeMillis();
				}
			} catch (InterruptedException e) {
				// just go away
			}
			return stages.size() == 0;
		}
	}
}

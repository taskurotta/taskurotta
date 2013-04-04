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

	public BasicFlowArbiter(List<String> stages) {
		this.stages = stages;
	}

	@Override
	public void notify(String tag) {
		log.debug("notified about tag [{}]; {}", tag, stages);

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

	public String getCurrentStage() {
		return stages.get(0);
	}

	protected void pause(long millis) {
		try {
			stages.wait(millis);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	protected void resume() {
		stages.notifyAll();
	}

	protected abstract void process(String tag);
}

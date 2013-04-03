package ru.taskurotta.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by void 01.04.13 12:04
 */
public abstract class BasicFlowArbiter implements FlowArbiter {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final List<Stage> stages;

	private String lastTag;

	public BasicFlowArbiter(List<String> stages) {
		this.stages = new LinkedList<Stage>();
		for (String line : stages) {
			this.stages.add(new Stage(line));
		}
	}

	@Override
	public void notify(String tag) {
		log.debug("notified about tag [{}]; Stage list: {}", tag, stages);

		synchronized (stages) {
			Stage current = stages.get(0);

			if (current == null) {
				throw new IncorrectFlowException("Expected flow finished. Called with " + tag);
			}

			if (!current.remove(tag)) {
				throw new IncorrectFlowException("Wrong tag: expected "+ current +" but found {"+ tag +"}");
			}
			if (current.isEmpty()) {
				stages.remove(0);
			}

			process(tag);

			lastTag = tag;   // saved for waitForTag processes
			stages.notifyAll();
		}

	}

	public Stage getCurrentStage() {
		return stages.get(0);
	}

	protected void waitForTag(String tag, long timeToWait) {
		try {
			long endTime = System.currentTimeMillis() + timeToWait;
			while (!lastTag.equals(tag) && timeToWait > 0) {
				stages.wait(timeToWait);
				timeToWait = endTime - System.currentTimeMillis();
			}
			if (timeToWait <= 0) {
				throw new IncorrectFlowException("Tag "+ tag +" doesn't checked");
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

					log.debug("Stages: {}, time to wait: {}", stages, timeToWait);
				}
			} catch (InterruptedException e) {
				// just go away
			}
			return stages.size() == 0;
		}
	}

	private boolean checkTag(String tag) {
		Stage stage = stages.get(0);
		return stage != null && stage.contains(tag);
	}

}

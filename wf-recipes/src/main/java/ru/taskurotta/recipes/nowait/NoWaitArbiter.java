package ru.taskurotta.recipes.nowait;

import ru.taskurotta.test.BasicFlowArbiter;
import ru.taskurotta.test.IncorrectFlowException;

import java.util.List;

/**
 * Created by void 29.03.13 11:18
 */
public class NoWaitArbiter extends BasicFlowArbiter {

	public NoWaitArbiter(List<String> tags) {
		super(tags);
	}

	protected void process(String tag) {

		if ("taskB".equals(tag)) {
			String before = getCurrentStage();
			waitForTag("process", 10000);
			String after = getCurrentStage();

			if (before.equals(after)) {
				throw new IncorrectFlowException("Task with @NoWait doesn't started");
			}
		}

	}
}

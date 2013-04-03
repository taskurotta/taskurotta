package ru.taskurotta.recipes.nowait;

import ru.taskurotta.test.BasicFlowArbiter;
import ru.taskurotta.test.IncorrectFlowException;
import ru.taskurotta.test.Stage;

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
			waitForTag("process", 10000);
		}

	}
}

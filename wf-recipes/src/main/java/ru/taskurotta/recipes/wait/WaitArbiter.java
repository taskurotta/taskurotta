package ru.taskurotta.recipes.wait;

import ru.taskurotta.test.BasicFlowArbiter;

import java.util.List;

/**
 * Created by void 29.03.13 11:18
 */
public class WaitArbiter extends BasicFlowArbiter {

	public WaitArbiter(List<String> tags) {
		super(tags);
	}

	protected void process(String tag) {

/*
		if ("prepare".equals(tag)) {
			waitForTag("waitForStart", 10000);
		}
*/

	}
}

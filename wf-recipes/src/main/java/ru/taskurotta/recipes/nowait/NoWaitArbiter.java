package ru.taskurotta.recipes.nowait;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.test.BasicFlowArbiter;
import ru.taskurotta.test.FlowArbiter;
import ru.taskurotta.test.IncorrectFlowException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by void 29.03.13 11:18
 */
public class NoWaitArbiter extends BasicFlowArbiter {

	private static final String[] tags = {"start", "taskC", "taskB", "process", "taskE"};

	public NoWaitArbiter(List<String> tags) {
//		super(Arrays.asList(tags));
		super(tags);
	}

	protected void process(String tag) {

		if ("taskB".equals(tag)) {
			String before = getCurrentStage();
			pause(10000);
			String after = getCurrentStage();

			if (before.equals(after)) {
				throw new IncorrectFlowException("Task with @NoWait doesn't started");
			}
		}

		if ("process".equals(tag)) {
			resume();
		}
	}
}

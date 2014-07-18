package ru.taskurotta.client.internal;

import org.junit.Test;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;

import java.util.UUID;

/**
 * Created by void 17.09.13 18:52
 */
public class WaitTest extends AbstractTestStub {

	@Test
	public void waitTest() {
		UUID taskIdA = UUID.randomUUID();
		UUID taskIdB = UUID.randomUUID();
		UUID taskIdC = UUID.randomUUID();

		Task deciderTaskA = deciderTask(taskIdA, TaskType.DECIDER_START, "start");
		taskServer.startProcess(objectFactory.dumpTask(deciderTaskA));

		// poll task A
		pollDeciderTask(taskIdA);

		// release task A
		Task taskB = deciderTask(taskIdB, TaskType.DECIDER_ASYNCHRONOUS, "B");
		Task taskC = deciderTask(taskIdC, TaskType.DECIDER_START, "C", null,
				new Object[]{},
				new TaskOptions()
                        .setArgTypes(new ArgType[]{ArgType.WAIT})
                        .setPromisesWaitFor(new Promise[]{promise(taskB)}));

		// release decision with taskB and taskС
		release(taskIdA, null, taskB, taskC);

		// poll task B
		pollDeciderTask(taskIdB);

		assertEmptyQueue();
	}
}

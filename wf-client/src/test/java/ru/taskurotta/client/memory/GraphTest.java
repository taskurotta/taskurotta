package ru.taskurotta.client.memory;

import org.junit.Test;
import ru.taskurotta.core.Task;
import ru.taskurotta.transport.model.TaskType;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 30.07.13
 * Time: 11:36
 */
public class GraphTest extends AbstractTestStub {

    @Test
    public void duplicateDecisionRelease() {
        UUID taskIdA = UUID.randomUUID();
        UUID taskIdB = UUID.randomUUID();
        UUID taskIdBDuplicate = UUID.randomUUID();

        // start process
        startProcess(deciderTask(taskIdA, TaskType.DECIDER_START, "A"));

        // poll task A
        pollDeciderTask(taskIdA);

        // release task A
        Task taskB = deciderTask(taskIdB, TaskType.DECIDER_ASYNCHRONOUS, "B");
        Task taskBDuplicate = deciderTask(taskIdBDuplicate, TaskType.DECIDER_ASYNCHRONOUS, "B");

        // release decision with taskB
        release(taskIdA, null, new Task[]{taskB});

        // release decision with duplicate taskB
        release(taskIdA, null, new Task[]{taskBDuplicate});

        // poll task B
        pollDeciderTask(taskIdB);

        // should be empty queue
        assertEmptyQueue();
    }

	@org.junit.Ignore
	@Test
	public void chainTasks() {
		UUID taskIdA = UUID.randomUUID();
		UUID taskIdB = UUID.randomUUID();
		UUID taskIdC = UUID.randomUUID();
		UUID taskIdD = UUID.randomUUID();
		UUID taskIdE = UUID.randomUUID();

		// start process
		startProcess(deciderTask(taskIdA, TaskType.DECIDER_START, "A"));

		// poll task A
		pollDeciderTask(taskIdA);

		// release task A
		Task taskB = deciderTask(taskIdB, TaskType.DECIDER_ASYNCHRONOUS, "B");
		Task taskC = deciderTask(taskIdC, TaskType.DECIDER_ASYNCHRONOUS, "C");
		Task taskE = deciderTask(taskIdE, TaskType.DECIDER_ASYNCHRONOUS, "E", new Object[]{promise(taskB), promise(taskC)});
		// release decision with taskB and taskС and taskE
		release(taskIdA, null, new Task[]{taskB, taskC, taskE});

		// poll task B
		pollDeciderTask(taskIdB, taskIdC);
		// poll task C
		pollDeciderTask(taskIdB, taskIdC);
		assertEmptyQueue();

		// release task B
		Task taskD = deciderTask(taskIdD, TaskType.DECIDER_ASYNCHRONOUS, "D");
		// release decision with taskD and return taskD as promise
		release(taskIdB, promise(taskD), new Task[]{taskD});

		// poll task D
		pollDeciderTask(taskIdD);
		assertEmptyQueue();

		// release task C
		release(taskIdC, null, new Task[]{});

		// Task E should wait for release of task D
		assertEmptyQueue();
	}
}

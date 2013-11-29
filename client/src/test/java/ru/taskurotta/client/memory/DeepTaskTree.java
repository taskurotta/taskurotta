package ru.taskurotta.client.memory;

import org.junit.Test;
import ru.taskurotta.core.Task;
import ru.taskurotta.transport.model.TaskType;

import java.util.UUID;

/**
 * User: romario
 * Date: 7/25/13
 * Time: 4:16 PM
 */
public class DeepTaskTree extends AbstractTestStub {

    /**
     * - A -> B, C(B)
     * - B -> D
     * - D -> E
     * <p/>
     * <p/>
     * task C should be ready only after task E
     */
    @Test
    public void testDeepTree() {

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
        Task taskC = deciderTask(taskIdC, TaskType.DECIDER_ASYNCHRONOUS, "C", promise(taskB));

        release(taskIdA, null, taskB, taskC);


        // poll task B
        pollDeciderTask(taskIdB);

        assertEmptyQueue();

        // release task B
        Task deciderTaskD = deciderTask(taskIdD, TaskType.DECIDER_ASYNCHRONOUS, "D");

        release(taskIdB, promise(deciderTaskD), deciderTaskD);


        // poll task D
        pollDeciderTask(taskIdD);

		assertEmptyQueue();

        // release task D
        Task deciderTaskE = deciderTask(taskIdE, TaskType.DECIDER_ASYNCHRONOUS, "E");
        release(taskIdD, promise(deciderTaskE), deciderTaskE);


        // poll task E
        pollDeciderTask(taskIdE);

		assertEmptyQueue();

		// release task E
        release(taskIdE, 1);


        // poll task C
        pollDeciderTask(taskIdC);

        // release task C
        release(taskIdC, 1);

		assertEmptyQueue();
    }

	/**
	 * - A -> B, C, E(B,C)
	 * - B -> D
	 *
	 * task C will be released after task B
	 * task E should be ready only after task D
	 */
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
		Task taskE = deciderTask(taskIdE, TaskType.DECIDER_ASYNCHRONOUS, "E", promise(taskB), promise(taskC));
		// release decision with taskB and taskС and taskE
		release(taskIdA, null, taskB, taskC, taskE);

		// poll task (B or C)
		pollDeciderTask(taskIdB, taskIdC);
		// poll task (C or B)
		pollDeciderTask(taskIdB, taskIdC);
		assertEmptyQueue();

		// release task B
		Task taskD = deciderTask(taskIdD, TaskType.DECIDER_ASYNCHRONOUS, "D");
		// release decision with taskD and return taskD as promise
		release(taskIdB, promise(taskD), taskD);

		// poll task D
		pollDeciderTask(taskIdD);
		assertEmptyQueue();

		// release task C
		release(taskIdC, null);

		// Task E should wait for release of task D
		assertEmptyQueue();
	}
}

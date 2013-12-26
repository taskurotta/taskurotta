package ru.taskurotta.client.memory;

import org.junit.Test;
import ru.taskurotta.core.Task;
import ru.taskurotta.internal.core.TaskType;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 30.07.13
 * Time: 11:36
 */
public class GraphTest extends AbstractTestStub {

	/**
	 * - A -> B
	 * - A -> BDuplicate
	 *
	 * BDuplicate should be ignored
	 */
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
        release(taskIdA, null, taskB);

        // release decision with duplicate taskB
        release(taskIdA, null, taskBDuplicate);

        // poll task B
        pollDeciderTask(taskIdB);

        assertEmptyQueue();
    }

}

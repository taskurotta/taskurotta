package ru.taskurotta.client.internal;

import org.junit.Test;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * User: romario
 * Date: 3/29/13
 * Time: 3:37 PM
 */
public class DelayedTaskTest extends AbstractTestStub {

    /**
     * - A -> B
     * <p/>
     * task B is delayed to 3 seconds
     */
    @Test
    public void testDelay() throws InterruptedException {

        UUID taskAId = UUID.randomUUID();
        Task deciderTaskA = deciderTask(taskAId, TaskType.DECIDER_START, "start");
        taskServer.startProcess(objectFactory.dumpTask(deciderTaskA));

        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(TestDecider.class));

        Task taskAFromQueue = deciderTaskSpreader.poll();

        // should be task A
        assertEquals(taskAId, taskAFromQueue.getId());

        // create B
        UUID taskBId = UUID.randomUUID();
        long timeout = 3000l;
        Task deciderTaskB = deciderTask(taskBId, TaskType.DECIDER_START, "startB", System.currentTimeMillis() + timeout);

        assertTrue(deciderTaskB.getStartTime() != 0L);

        TaskDecision taskADecision = new TaskDecisionImpl(taskAId, processId, null, null, new Task[]{deciderTaskB});
        deciderTaskSpreader.release(taskADecision);

        Task taskBFromQueue = deciderTaskSpreader.poll();
        assertNull(taskBFromQueue);

        Thread.sleep((long) (timeout * 1.5));

        taskBFromQueue = deciderTaskSpreader.poll();

        assertNotNull(taskBFromQueue);

        assertEquals(taskBId, taskBFromQueue.getId());

    }
}

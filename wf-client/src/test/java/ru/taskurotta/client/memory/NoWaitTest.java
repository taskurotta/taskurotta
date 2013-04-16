package ru.taskurotta.client.memory;

import org.junit.Test;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.core.TaskOptionsImpl;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * User: romario
 * Date: 3/26/13
 * Time: 2:07 PM
 */
public class NoWaitTest extends AbstractTestStub {

    /**
     * - A -> B, C, D(@NoWait B, C)
     * - D -> E(B)
     * <p/>
     * D can be queued before task B has been processed
     * E should be queued after task B has been processed
     */
    @Test
    public void testNoWait() {

        UUID taskAId = UUID.randomUUID();
        Task deciderTaskA = deciderTask(taskAId, TaskType.DECIDER_START, "start");
        taskServer.startProcess(objectFactory.dumpTask(deciderTaskA));

        TaskSpreader deciderTaskSpreader = taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(TestDecider.class));

        Task taskAFromQueue = deciderTaskSpreader.poll();

        // should be task A
        assertEquals(taskAId, taskAFromQueue.getId());

        // task should be in "process" state
        assertTrue(isTaskInProgress(taskAId));

        // create B, C, D tasks
        UUID taskBId = UUID.randomUUID();
        Task deciderTaskB = deciderTask(taskBId, TaskType.DECIDER_START, "startB");

        UUID taskCId = UUID.randomUUID();
        Task deciderTaskC = deciderTask(taskCId, TaskType.DECIDER_START, "startC");

        UUID taskDId = UUID.randomUUID();
        Task deciderTaskD = deciderTask(taskDId, TaskType.DECIDER_START, "startD",
                new Object[]{promise(deciderTaskB), promise(deciderTaskC)},
                new TaskOptionsImpl(new ArgType[]{ArgType.NO_WAIT, ArgType.NONE}));

        TaskDecision taskADecision = new TaskDecisionImpl(taskAId, processId, null, new Task[]{deciderTaskB,
                deciderTaskC, deciderTaskD});
        deciderTaskSpreader.release(taskADecision);

        // taskC and taskB may be pooled in different order

        Task taskCFromQueue = null;
        Task taskBFromQueue = null;

        Task task1 = deciderTaskSpreader.poll();
        Task task2 = deciderTaskSpreader.poll();
        if (task1.getId().equals(taskCId)) {
            taskCFromQueue = task1;
        }
        if (task1.getId().equals(taskBId)) {
            taskBFromQueue = task1;
        }
        if (task2.getId().equals(taskCId)) {
            taskCFromQueue = task2;
        }
        if (task2.getId().equals(taskBId)) {
            taskBFromQueue = task2;
        }

        assertEquals(taskCId, taskCFromQueue.getId());
        assertEquals(taskBId, taskBFromQueue.getId());

        Task taskDFromQueue = deciderTaskSpreader.poll();

        assertNull(taskDFromQueue);

        // release task C and task D should be queued

        TaskDecision taskCDecision = new TaskDecisionImpl(taskCId, processId, null, null);
        deciderTaskSpreader.release(taskCDecision);

        taskDFromQueue = deciderTaskSpreader.poll();

        assertNotNull(taskDFromQueue);

        // release task D = create task E(B)

        UUID taskEId = UUID.randomUUID();
        Task deciderTaskE = deciderTask(taskEId, TaskType.DECIDER_START, "startE",
                new Object[]{promise(deciderTaskB)},
                null);

        TaskDecision taskDDecision = new TaskDecisionImpl(taskDId, processId, null, new Task[]{deciderTaskE});
        deciderTaskSpreader.release(taskDDecision);

        // should be empty queue. we are still waiting task B

        Task taskEFromQueue = deciderTaskSpreader.poll();

        assertNull(taskEFromQueue);

        // release task B

        TaskDecision taskBDecision = new TaskDecisionImpl(taskBId, processId, null, null);
        deciderTaskSpreader.release(taskBDecision);

        taskEFromQueue = deciderTaskSpreader.poll();

        // should be task E
        assertEquals(taskEId, taskEFromQueue.getId());

        // should be empty queue

        Task task = deciderTaskSpreader.poll();

        assertNull(task);
    }

}

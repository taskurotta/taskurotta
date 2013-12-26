package ru.taskurotta.client.internal;

import org.junit.Test;
import ru.taskurotta.core.Task;
import ru.taskurotta.internal.core.TaskOptionsImpl;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;

import java.util.UUID;

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
        UUID taskBId = UUID.randomUUID();
        UUID taskCId = UUID.randomUUID();
        UUID taskDId = UUID.randomUUID();
        UUID taskEId = UUID.randomUUID();

        startProcess(deciderTask(taskAId, TaskType.DECIDER_START, "start"));

        // should be task A
        pollDeciderTask(taskAId);

        // task should be in "process" state
        assertTaskInProgress(taskAId);

        // create B, C, D tasks
        Task deciderTaskB = deciderTask(taskBId, TaskType.DECIDER_ASYNCHRONOUS, "startB");
        Task deciderTaskC = deciderTask(taskCId, TaskType.DECIDER_ASYNCHRONOUS, "startC");
        Task deciderTaskD = deciderTask(taskDId, TaskType.DECIDER_ASYNCHRONOUS, "startD", null,
                new Object[]{promise(deciderTaskB), promise(deciderTaskC)},
                new TaskOptionsImpl(new ArgType[]{ArgType.NO_WAIT, ArgType.NONE}));

        release(taskAId, null, deciderTaskB, deciderTaskC, deciderTaskD);

        // taskC and taskB may be pooled in different order
        // poll task (B or C)
        pollDeciderTask(taskBId, taskCId);
        // poll task (C or B)
        pollDeciderTask(taskBId, taskCId);
        assertEmptyQueue();

        // release task C and task D should be queued
        release(taskCId, null);

        // should be task D
        pollDeciderTask(taskDId);

        // release task D = create task E(B)
        Task taskE = deciderTask(taskEId, TaskType.DECIDER_ASYNCHRONOUS, "startE", promise(deciderTaskB));
        release(taskDId, null, taskE);
        assertEmptyQueue();

        // release task B
        release(taskBId, null);

        // should be task E
        pollDeciderTask(taskEId);
        assertEmptyQueue();
    }

}

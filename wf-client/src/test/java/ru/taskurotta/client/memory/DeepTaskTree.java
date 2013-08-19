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
    public void testNoWait() {

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
        Task taskC = deciderTask(taskIdC, TaskType.DECIDER_ASYNCHRONOUS, "C", new Object[]{promise(taskB)});

        release(taskIdA, null, new Task[]{taskB, taskC});


        // poll task B
        pollDeciderTask(taskIdB);

        // should be empty queue
        pollDeciderTask(null);

        // release task B
        Task deciderTaskD = deciderTask(taskIdD, TaskType.DECIDER_ASYNCHRONOUS, "D");

        release(taskIdB, promise(deciderTaskD), new Task[]{deciderTaskD});


        // poll task D
        pollDeciderTask(taskIdD);

        // should be empty queue
        pollDeciderTask(null);

        // release task D
        Task deciderTaskE = deciderTask(taskIdE, TaskType.DECIDER_ASYNCHRONOUS, "E");
        release(taskIdD, promise(deciderTaskE), new Task[]{deciderTaskE});


        // poll task E
        pollDeciderTask(taskIdE);

        // should be empty queue
        pollDeciderTask(null);

        // release task E
        release(taskIdE, 1, null);


        // poll task C
        pollDeciderTask(taskIdC);

        // release task C
        release(taskIdC, 1, null);

        // should be empty queue
        pollDeciderTask(null);
    }

}

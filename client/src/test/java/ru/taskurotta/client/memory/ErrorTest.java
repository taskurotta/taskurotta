package ru.taskurotta.client.memory;

import org.junit.Test;
import ru.taskurotta.core.Fail;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.internal.core.TaskOptionsImpl;
import ru.taskurotta.transport.model.ArgType;
import ru.taskurotta.transport.model.TaskType;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by void 03.12.13 13:43
 */
public class ErrorTest extends AbstractTestStub {

    @Test
    public void testError() {
        UUID taskAId = UUID.randomUUID();
        UUID taskBId = UUID.randomUUID();
        UUID taskCId = UUID.randomUUID();

        startProcess(deciderTask(taskAId, TaskType.DECIDER_START, "start"));

        // should be task A
        pollDeciderTask(taskAId);

        // task should be in "process" state
        assertTaskInProgress(taskAId);

        // create B, C tasks
        Task deciderTaskB = deciderTask(taskBId, TaskType.DECIDER_ASYNCHRONOUS, "startB", true, new Object[]{}, null);
        Task deciderTaskC = deciderTask(taskCId, TaskType.DECIDER_ASYNCHRONOUS, "startC", false,
                new Object[]{promise(deciderTaskB)},
                new TaskOptionsImpl(new ArgType[]{ArgType.NONE}));

        release(taskAId, null, deciderTaskB, deciderTaskC);

        // should be task B
        pollDeciderTask(taskBId);

        // release task B                                                               En
        release(taskBId, new NesmoglaException("NeshmoglaException occurred"));

        // should be task B
        Task taskC = pollDeciderTask(taskCId);
        assertEmptyQueue();

        Promise promise = (Promise)taskC.getArgs()[0];
        assertTrue("Promise contains error", promise.containsFail());

        Fail fail = promise.getFail();
        assertTrue(fail.instanceOf("java.lang.RuntimeException"));
        assertTrue(fail.instanceOf("java.lang.Throwable"));
        assertFalse(fail.instanceOf("java.lang.NullPointerException"));
    }

    public class NesmoglaException extends RuntimeException {
        public NesmoglaException() {
        }

        public NesmoglaException(String message) {
            super(message);
        }

        public NesmoglaException(String message, Throwable cause) {
            super(message, cause);
        }

        public NesmoglaException(Throwable cause) {
            super(cause);
        }
    }


}

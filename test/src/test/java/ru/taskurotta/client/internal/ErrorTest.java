package ru.taskurotta.client.internal;

import org.junit.Test;
import ru.taskurotta.core.Fail;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.internal.core.TaskType;

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
        Task deciderTaskB = deciderTask(taskBId, TaskType.DECIDER_ASYNCHRONOUS, "startB",
                new String[]{"java.lang.RuntimeException"}, new Object[]{}, null);
        Task deciderTaskC = deciderTask(taskCId, TaskType.DECIDER_ASYNCHRONOUS, "startC", null,
                new Object[]{promise(deciderTaskB)},
                TaskOptions.builder().withArgTypes(new ArgType[]{ArgType.NONE}).build());

        release(taskAId, null, deciderTaskB, deciderTaskC);

        // should be task B
        pollDeciderTask(taskBId);

        // release task B                                                               En
        release(taskBId, new NesmoglaException("NeshmoglaException occurred"));

        // should be task C
        Task taskC = pollDeciderTask(taskCId);
        assertEmptyQueue();

        Promise promise = (Promise)taskC.getArgs()[0];
        assertTrue("Promise contains error", promise.hasFail());

        Fail fail = promise.getFail();
        assertTrue(fail.instanceOf("java.lang.RuntimeException"));
        assertTrue(fail.instanceOf("java.lang.Throwable"));
        assertFalse(fail.instanceOf("java.lang.NullPointerException"));
    }

    @Test
    public void testErrorWithoutRestrictionOnFailType() {
        UUID taskAId = UUID.randomUUID();
        UUID taskBId = UUID.randomUUID();
        UUID taskCId = UUID.randomUUID();

        startProcess(deciderTask(taskAId, TaskType.DECIDER_START, "start"));

        // should be task A
        pollDeciderTask(taskAId);

        // task should be in "process" state
        assertTaskInProgress(taskAId);

        // create B, C tasks
        Task deciderTaskB = deciderTask(taskBId, TaskType.DECIDER_ASYNCHRONOUS, "startB",
                new String[]{}, new Object[]{}, null);   // empty array of types as restriction!
        Task deciderTaskC = deciderTask(taskCId, TaskType.DECIDER_ASYNCHRONOUS, "startC", null,
                new Object[]{promise(deciderTaskB)},
                TaskOptions.builder().withArgTypes(new ArgType[]{ArgType.NONE}).build());

        release(taskAId, null, deciderTaskB, deciderTaskC);

        // should be task B
        pollDeciderTask(taskBId);

        // release task B                                                               En
        release(taskBId, new NesmoglaException("NeshmoglaException occurred"));

        // should be task C
        Task taskC = pollDeciderTask(taskCId);
        assertEmptyQueue();

        Promise promise = (Promise)taskC.getArgs()[0];
        assertTrue("Promise contains error", promise.hasFail());

        Fail fail = promise.getFail();
        assertTrue(fail.instanceOf("java.lang.RuntimeException"));
        assertTrue(fail.instanceOf("java.lang.Throwable"));
        assertFalse(fail.instanceOf("java.lang.NullPointerException"));
    }

    @Test
    public void testErrorWithWrongException() {
        UUID taskAId = UUID.randomUUID();
        UUID taskBId = UUID.randomUUID();
        UUID taskCId = UUID.randomUUID();

        startProcess(deciderTask(taskAId, TaskType.DECIDER_START, "start"));

        // should be task A
        pollDeciderTask(taskAId);

        // task should be in "process" state
        assertTaskInProgress(taskAId);

        // create B, C tasks
        Task deciderTaskB = deciderTask(taskBId, TaskType.DECIDER_ASYNCHRONOUS, "startB",
                new String[]{"java.lang.NullPointerException"}, new Object[]{}, null);
        Task deciderTaskC = deciderTask(taskCId, TaskType.DECIDER_ASYNCHRONOUS, "startC", null,
                new Object[]{promise(deciderTaskB)},
                TaskOptions.builder().withArgTypes(new ArgType[]{ArgType.NONE}).build());

        release(taskAId, null, deciderTaskB, deciderTaskC);

        // should be task B
        pollDeciderTask(taskBId);

        // release task B                                                               En
        release(taskBId, new NesmoglaException("NeshmoglaException occurred"));

        // process should be stopped
        assertEmptyQueue();
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

package ru.taskurotta;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.test.TestTasks;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * User: stukushin
 * Date: 25.01.13
 * Time: 11:23
 */
public class ExecuteWorkerTest {

    private RuntimeProcessor runtimeProcessor;
    private static boolean flag = false;
    private static UUID processId = UUID.randomUUID();

    @Worker
    public static interface SimpleWorker {
        public int max(int a, int b);

        public int fibonacci(int n);

        public void voidMethod();
    }

    public static class SimpleWorkerImpl implements SimpleWorker {

        @Override
        public int max(int a, int b) {
            return Math.max(a, b);
        }

        @Override
        public int fibonacci(int n) {
            return (n <= 2 ? 1 : fibonacci(n - 1) + fibonacci(n - 2));
        }

        @Override
        public void voidMethod() {
            flag = true;
        }
    }

    @Before
    public void setUp() {
        RuntimeProvider runtimeProvider = RuntimeProviderManager.getRuntimeProvider();

        runtimeProcessor = runtimeProvider.getRuntimeProcessor(new SimpleWorkerImpl());
    }

    @Test
    public void testSimpleMethod() {
        TaskTarget taskTarget = new TaskTargetImpl(TaskType.WORKER, SimpleWorker.class.getName(), "1.0", "max");
        Task task = TestTasks.newInstance(processId, taskTarget, new Object[]{5, 6});
        TaskDecision taskDecision = runtimeProcessor.execute(task);
        assertEquals(taskDecision.getValue(), 6);
    }

    @Test
    public void testRecursiveMethod() {
        TaskTarget taskTarget = new TaskTargetImpl(TaskType.WORKER, SimpleWorker.class.getName(), "1.0", "fibonacci");
        Task task = TestTasks.newInstance(processId, taskTarget, new Object[]{4});
        TaskDecision taskDecision = runtimeProcessor.execute(task);
        assertEquals(taskDecision.getValue(), 3);
    }

    @Test
    public void testVoidMethod() {
        TaskTarget taskTarget = new TaskTargetImpl(TaskType.WORKER, SimpleWorker.class.getName(), "1.0", "voidMethod");
        Task task = TestTasks.newInstance(processId, taskTarget, new Object[]{});

        assertFalse(flag);
        TaskDecision taskDecision = runtimeProcessor.execute(task);
        assertTrue(flag);
        assertNull(taskDecision.getValue());
    }
}

package ru.taskurotta.internal;

import org.junit.Assert;
import org.junit.Test;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.RuntimeProviderManager;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.UndefinedActorException;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.test.TestTasks;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 24.01.13
 * Time: 17:20
 */
public class GeneralRuntimeProcessorTest {
    // decider
    // ==================

    @Decider
    public static interface SimpleDecider {
        @Execute
        public void start();
    }


    public static class SimpleDeciderImpl implements SimpleDecider {
        @Override
        public void start() {
            throw new RuntimeException("test exception");
        }
    }

    @Test
    public void testUndefinedExecuteTask() throws Exception {
        RuntimeProvider runtimeProvider = RuntimeProviderManager.getRuntimeProvider();

        RuntimeProcessor runtimeProcessor = runtimeProvider.getRuntimeProcessor(new SimpleDeciderImpl());

        TaskTarget taskTarget = new TaskTargetImpl(TaskType.DECIDER_START, SimpleDecider.class.getName(), "1.0", "start1");
        Task task = TestTasks.newInstance(UUID.randomUUID(), taskTarget, null);

        TaskDecision taskDecision = runtimeProcessor.execute(task, null);
        Assert.assertTrue(taskDecision.isError());
        Assert.assertEquals(UndefinedActorException.class, taskDecision.getException().getClass());
    }

    @Test
    public void testExecuteTaskWithException() throws Exception {
        RuntimeProvider runtimeProvider = RuntimeProviderManager.getRuntimeProvider();

        RuntimeProcessor runtimeProcessor = runtimeProvider.getRuntimeProcessor(new SimpleDeciderImpl());

        TaskTarget taskTarget = new TaskTargetImpl(TaskType.DECIDER_START, SimpleDecider.class.getName(), "1.0", "start");
        Task task = TestTasks.newInstance(UUID.randomUUID(), taskTarget, null);

        TaskDecision taskDecision = runtimeProcessor.execute(task, null);
        Assert.assertTrue(taskDecision.isError());
        Assert.assertEquals(RuntimeException.class, taskDecision.getException().getClass());
    }

}

package ru.taskurotta.bootstrap.profiler;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.bootstrap.TestDecider;
import ru.taskurotta.bootstrap.TestDeciderClient;
import ru.taskurotta.bootstrap.TestWorker;
import ru.taskurotta.bootstrap.TestWorkerImpl;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.client.memory.ClientServiceManagerMemory;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.GeneralRuntimeProvider;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 02.04.13
 * Time: 18:59
 */
public class SimpleProfilerTest {

    private static Profiler simpleProfiler;

    @BeforeClass
    public static void setUp() throws Exception {
        simpleProfiler = new SimpleProfiler(TestWorker.class);
    }

    @Test
    public void testDecorateRuntimeProcessor() throws Exception {
        RuntimeProvider runtimeProvider = new GeneralRuntimeProvider();
        RuntimeProcessor runtimeProcessor = runtimeProvider.getRuntimeProcessor(new TestWorkerImpl());

        TaskTarget taskTarget = new TaskTargetImpl(TaskType.WORKER, TestWorker.class.getName(), "1.0", "sum");
        Task task = new TaskImpl(UUID.randomUUID(), taskTarget, System.currentTimeMillis(), 1, new Object[]{1, 2}, null);
        TaskDecision taskDecision = runtimeProcessor.execute(task);

        RuntimeProcessor decorateRuntimeProcessor = simpleProfiler.decorate(runtimeProcessor);
        TaskDecision taskDecisionFromDecorateRuntimeProcessor = decorateRuntimeProcessor.execute(task);

        assertEquals(taskDecision, taskDecisionFromDecorateRuntimeProcessor);
    }

    @Test
    public void testDecorateTaskSpreader() throws Exception {
        Task task = getTaskSpreader().pull();

        TaskSpreader decorateTaskSpreader = simpleProfiler.decorate(getTaskSpreader());
        Task taskFromDecorateTaskSpreader = decorateTaskSpreader.pull();

        assertEquals(task.getTarget(), taskFromDecorateTaskSpreader.getTarget());
    }

    private TaskSpreader getTaskSpreader() {
        ClientServiceManager clientServiceManager = new ClientServiceManagerMemory();
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        TestDeciderClient testDecider = deciderClientProvider.getDeciderClient(TestDeciderClient.class);
        testDecider.start(1, 2);

        TaskSpreaderProvider taskSpreaderProvider = clientServiceManager.getTaskSpreaderProvider();

        return taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(TestDecider.class));
    }
}

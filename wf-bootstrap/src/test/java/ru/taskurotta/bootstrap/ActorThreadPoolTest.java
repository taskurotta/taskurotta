package ru.taskurotta.bootstrap;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.RuntimeProviderManager;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.internal.TaskSpreaderCommon;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.GeneralRuntimeProcessor;
import ru.taskurotta.policy.retry.BlankRetryPolicy;
import ru.taskurotta.policy.retry.LinearRetryPolicy;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 14.05.13
 * Time: 17:30
 */
public class ActorThreadPoolTest {

    class SimpleRuntimeProcessor implements RuntimeProcessor {

        @Override
        public TaskDecision execute(Task task) {
            return null;
        }

        @Override
        public Task[] execute(UUID processId, Runnable runnable) {
            return new Task[0];
        }
    }

    class SimpleTaskSpreader implements TaskSpreader {

        @Override
        public Task poll() {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void release(TaskDecision taskDecision) {

        }
    }

    private ActorThreadPool actorThreadPool;
    private ActorExecutor actorExecutor;
    private int size = 10;

    @Before
    public void setUp() throws Exception {
        actorThreadPool = new ActorThreadPool(TestWorker.class, size);

        Profiler profiler = new SimpleProfiler();
        LinearRetryPolicy retryPolicy = new LinearRetryPolicy(1);
        retryPolicy.setMaximumAttempts(1);
        Inspector inspector = new Inspector(retryPolicy, actorThreadPool);

        actorExecutor = new ActorExecutor(profiler, inspector, new SimpleRuntimeProcessor(), new SimpleTaskSpreader());

        actorThreadPool.start(actorExecutor);
    }

    @Test
    public void testStart() throws Exception {
        TimeUnit.SECONDS.sleep(1);

        assertEquals(size, actorThreadPool.getActiveActorExecutorThreadCount());
    }

    @Test
    public void testMute() throws Exception {
        TimeUnit.SECONDS.sleep(5);

        assertEquals(1, actorThreadPool.getActiveActorExecutorThreadCount());
    }

    @Test
    public void testWake() throws Exception {
        TimeUnit.SECONDS.sleep(5);

        actorThreadPool.wake();

        TimeUnit.SECONDS.sleep(1);

        assertEquals(10, actorThreadPool.getActiveActorExecutorThreadCount());
    }
}

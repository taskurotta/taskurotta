package ru.taskurotta.bootstrap;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.pool.ActorMultiThreadPool;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.Heartbeat;
import ru.taskurotta.policy.retry.LinearRetryPolicy;
import ru.taskurotta.util.DuplicationErrorSuppressor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
        public TaskDecision execute(Task task, Heartbeat heartbeat) {
            return null;
        }

        @Override
        public Task[] execute(UUID taskId, UUID processId, Heartbeat heartbeat, Runnable runnable) {
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
        public void release(TaskDecision taskDecision) {}

        @Override
        public void updateTimeout(UUID taskId, UUID processId, long timeout) {

        }
    }

    private ActorMultiThreadPool actorThreadPool;
    private int size = 10;

    @Before
    public void setUp() throws Exception {
        actorThreadPool = new ActorMultiThreadPool(TestWorker.class.getName(), null, size, 60000l);

        Profiler profiler = new SimpleProfiler();
        LinearRetryPolicy retryPolicy = new LinearRetryPolicy(1);
        retryPolicy.setMaximumAttempts(2);
        Inspector inspector = new Inspector(retryPolicy, actorThreadPool);

        ActorExecutor actorExecutor = new ActorExecutor(profiler, inspector, new SimpleRuntimeProcessor(), new SimpleTaskSpreader(), new ConcurrentHashMap<>(), 0, new DuplicationErrorSuppressor(0, false), 0);

        actorThreadPool.start(actorExecutor);
    }

    @Test
    public void testStart() throws Exception {
        TimeUnit.SECONDS.sleep(1);
        assertEquals(size, actorThreadPool.getCurrentSize());
    }

    @Test
    public void testMute() throws Exception {
        TimeUnit.SECONDS.sleep(5);
        assertEquals(1, actorThreadPool.getCurrentSize());
    }

    @Test
    public void testWake() throws Exception {
        TimeUnit.SECONDS.sleep(5);//ensure pool is sleepy
        assertEquals(1, actorThreadPool.getCurrentSize());

        actorThreadPool.wake();
        assertEquals(10, actorThreadPool.getCurrentSize());
    }
}

package ru.taskurotta.bootstrap.pool;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.bootstrap.ActorExecutor;
import ru.taskurotta.bootstrap.Inspector;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.policy.retry.LinearRetryPolicy;

import java.sql.Time;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ActorMultiThreadPoolTest {

    private class SimpleRuntimeProcessor implements RuntimeProcessor {
        @Override
        public TaskDecision execute(Task task) {
            return null;
        }

        @Override
        public Task[] execute(UUID processId, Runnable runnable) {
            return new Task[0];
        }
    }

    private class SimpleTaskSpreader implements TaskSpreader {
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

    @Worker
    private interface TestWorker {
        int sum(int a, int b);
    }

    @WorkerClient(worker = TestWorker.class)
    private interface TestWorkerClient {
        int sum(int a, int b);
    }

    private class TestWorkerImpl implements TestWorker {
        @Override
        public int sum(int a, int b) {
            return a + b;
        }
    }

    private ActorMultiThreadPool actorThreadPool;
    private int size = 4;
    private long shutdownTimeoutMillis = 5 * 1000L;

    @Before
    public void setUp() throws Exception {
        actorThreadPool = new ActorMultiThreadPool(TestWorker.class.getName(), null, size, shutdownTimeoutMillis);

        Profiler profiler = new SimpleProfiler();
        LinearRetryPolicy retryPolicy = new LinearRetryPolicy(1);
        retryPolicy.setMaximumAttempts(2);
        Inspector inspector = new Inspector(retryPolicy, actorThreadPool);

        ActorExecutor actorExecutor =
                new ActorExecutor(profiler, inspector, new SimpleRuntimeProcessor(), new SimpleTaskSpreader());

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
        assertEquals(size, actorThreadPool.getCurrentSize());
    }

    @Test
    public void testShutdown() throws Exception {
        actorThreadPool.shutdown();

        TimeUnit.MILLISECONDS.sleep(shutdownTimeoutMillis);
        assertEquals(0, actorThreadPool.getCurrentSize());
    }
}

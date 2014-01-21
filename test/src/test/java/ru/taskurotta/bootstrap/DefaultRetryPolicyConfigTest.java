package ru.taskurotta.bootstrap;

import org.junit.Assert;
import org.junit.Test;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.bootstrap.config.DefaultRetryPolicyConfig;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.internal.GeneralRuntimeProvider;
import ru.taskurotta.policy.retry.LinearRetryPolicy;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 08.04.13
 * Time: 14:31
 */
public class DefaultRetryPolicyConfigTest {

    @Test
    public void testGetRetryPolicy() {
        long initialRetryIntervalSeconds = 20;

        Properties properties = new Properties();
        properties.setProperty("initialRetryIntervalSeconds", String.valueOf(initialRetryIntervalSeconds));

        DefaultRetryPolicyConfig retryPolicyConfig = new DefaultRetryPolicyConfig();
        retryPolicyConfig.setClass(LinearRetryPolicy.class.getName());
        retryPolicyConfig.setProperties(properties);

        assertEquals(new LinearRetryPolicy(initialRetryIntervalSeconds), retryPolicyConfig.getRetryPolicy());
    }

    @Test
    public void testPollRetryPolicy() throws Exception {
        TestTaskSpreader taskSpreader = new TestTaskSpreader();

        RuntimeProvider runtimeProvider = new GeneralRuntimeProvider();
        RuntimeProcessor runtimeProcessor = runtimeProvider.getRuntimeProcessor(new TestWorkerImpl());

        Profiler profiler = new SimpleProfiler();
        RetryPolicy retryPolicy = new LinearRetryPolicy(2);

        MockActorThreadPool actorExecutorsPool = new MockActorThreadPool(TestWorker.class, 1);
        Inspector inspector = new Inspector(retryPolicy, actorExecutorsPool);

        ActorExecutor actorExecutor = new ActorExecutor(profiler, inspector, runtimeProcessor, taskSpreader);
        actorExecutorsPool.start(actorExecutor);

        TimeUnit.SECONDS.sleep(5);

        actorExecutorsPool.shutdown();

        List<Integer> retryTimeouts = new ArrayList<Integer>();
        retryTimeouts.add(0);
        retryTimeouts.add(2);
        retryTimeouts.add(2);

        Assert.assertEquals(retryTimeouts, taskSpreader.timeouts);
    }
}

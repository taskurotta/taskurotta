package ru.taskurotta.bootstrap.config;

import org.junit.Test;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.bootstrap.ActorExecutor;
import ru.taskurotta.bootstrap.PolicyArbiter;
import ru.taskurotta.bootstrap.TestDecider;
import ru.taskurotta.bootstrap.TestRuntimeProcessor;
import ru.taskurotta.bootstrap.TestTaskSpreader;
import ru.taskurotta.bootstrap.TestWorkerImpl;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.GeneralRuntimeProvider;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.policy.retry.LinearRetryPolicy;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

        Profiler profiler = new SimpleProfiler(TestDecider.class);
        RetryPolicy retryPolicy = new LinearRetryPolicy(2);
        PolicyArbiter policyArbiter = new PolicyArbiter(retryPolicy);

        ActorExecutor actorExecutor = new ActorExecutor(profiler, policyArbiter, runtimeProcessor, taskSpreader);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(actorExecutor);

        TimeUnit.SECONDS.sleep(5);
        actorExecutor.stop();
        executorService.shutdown();

        List<Integer> retryTimeouts = new ArrayList<Integer>();
        retryTimeouts.add(0);
        retryTimeouts.add(0);
        retryTimeouts.add(0);
        retryTimeouts.add(2);
        retryTimeouts.add(2);

        assertEquals(retryTimeouts, taskSpreader.timeouts);
    }

    @Test
    public void testExecuteRetryPolicy() throws Exception {
        TaskTarget taskTarget = new TaskTargetImpl(TaskType.DECIDER_START, TestDecider.class.getName(), "1.0", "start");
        Task task = new TaskImpl(UUID.randomUUID(), UUID.randomUUID(), taskTarget, System.currentTimeMillis(), 0, new Object[]{1, 2}, null);
        List<Task> tasks = new ArrayList<Task>();
        tasks.add(task);

        TestTaskSpreader taskSpreader = new TestTaskSpreader(tasks);

        TestRuntimeProcessor runtimeProcessor = new TestRuntimeProcessor();

        Profiler profiler = new SimpleProfiler(TestDecider.class);
        RetryPolicy retryPolicy = new LinearRetryPolicy(2);
        PolicyArbiter policyArbiter = new PolicyArbiter(retryPolicy);

        ActorExecutor actorExecutor = new ActorExecutor(profiler, policyArbiter, runtimeProcessor, taskSpreader);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(actorExecutor);

        TimeUnit.SECONDS.sleep(5);
        actorExecutor.stop();
        executorService.shutdown();

        List<Integer> retryTimeouts = new ArrayList<Integer>();
        retryTimeouts.add(0);
        retryTimeouts.add(0);
        retryTimeouts.add(0);
        retryTimeouts.add(2);
        retryTimeouts.add(2);

        assertEquals(retryTimeouts, runtimeProcessor.timeouts);
    }
}

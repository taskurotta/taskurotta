package ru.taskurotta.bootstrap;

import org.junit.Test;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.policy.retry.LinearRetryPolicy;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * User: stukushin
 * Date: 10.04.13
 * Time: 15:15
 */
public class PolicyArbiterTest {
    @Test
    public void testContinueAfterPoll() throws Exception {
        int initialRetryIntervalSeconds = 2;
        PolicyArbiter policyArbiter = new PolicyArbiter(new LinearRetryPolicy(initialRetryIntervalSeconds));

        long start = System.currentTimeMillis();
        policyArbiter.continueAfterPoll(null);
        policyArbiter.continueAfterPoll(null);
        policyArbiter.continueAfterPoll(null);
        assertEquals(initialRetryIntervalSeconds, (System.currentTimeMillis() - start) / 1000);

        TaskTarget taskTarget = new TaskTargetImpl(TaskType.DECIDER_START, TestDecider.class.getName(), "1.0", "start");
        Task task = new TaskImpl(UUID.randomUUID(), UUID.randomUUID(), taskTarget, System.currentTimeMillis(), 0, new Object[]{1,2}, null);

        start = System.currentTimeMillis();
        policyArbiter.continueAfterPoll(task);
        assertEquals(0, (System.currentTimeMillis() - start) / 1000);
    }

    @Test
    public void testContinueAfterExecute() throws Exception {
        int initialRetryIntervalSeconds = 2;
        PolicyArbiter policyArbiter = new PolicyArbiter(new LinearRetryPolicy(initialRetryIntervalSeconds));

        long start = System.currentTimeMillis();
        TaskDecision taskDecisionError = new TaskDecisionImpl(UUID.randomUUID(), UUID.randomUUID(), new Throwable(), null);
        policyArbiter.continueAfterExecute(taskDecisionError);
        policyArbiter.continueAfterExecute(taskDecisionError);
        policyArbiter.continueAfterExecute(taskDecisionError);
        assertEquals(initialRetryIntervalSeconds, (System.currentTimeMillis() - start) / 1000);

        TaskDecision taskDecisionCorrect = new TaskDecisionImpl(UUID.randomUUID(), UUID.randomUUID(), new Object(), null);
        start = System.currentTimeMillis();
        policyArbiter.continueAfterExecute(taskDecisionCorrect);
        assertEquals(0, (System.currentTimeMillis() - start) / 1000);
    }
}

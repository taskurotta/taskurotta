package ru.taskurotta.server.recovery;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskType;
import ru.taskurotta.util.ActorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TestRetryEnqueueRecovery {

    private static final Logger logger = LoggerFactory.getLogger(TestRetryEnqueueRecovery.class);
    private int timeout = 100;
    private TimeUnit timeoutUnit = TimeUnit.MILLISECONDS;

    @Test
    public void testTaskBackendRecovery() {
        RecoveryFactory recoveryFactory = new RecoveryFactory(timeout, timeoutUnit);
        performRecoveryCheck(recoveryFactory.getTaskServer(), recoveryFactory.getRecoveryProcess(TaskBackend.class), 1);
    }

    @Ignore
    @Test
    public void testQueueBackendRecovery() {
        RecoveryFactory recoveryFactory = new RecoveryFactory(timeout, timeoutUnit);
        performRecoveryCheck(recoveryFactory.getTaskServer(), recoveryFactory.getRecoveryProcess(QueueBackend.class), 1);
    }

    public void testUncheckedTimeoutsRecovery() {
        RecoveryFactory recoveryFactory = new RecoveryFactory(timeout, timeoutUnit);
        RetryEnqueueRecovery voidRecovery = recoveryFactory.getRecoveryProcess(TaskBackend.class);

        //never checked by task backend, so it should never be recovered
        TimeoutType[] unckeckedTypes = new TimeoutType[]{TimeoutType.TASK_POLL_TO_COMMIT};
        voidRecovery.setSupportedTimeouts(unckeckedTypes);

        performRecoveryCheck(recoveryFactory.getTaskServer(), voidRecovery, 0);
    }

    private void performRecoveryCheck(TaskServer taskServer, RetryEnqueueRecovery recovery, int expectedRecovered) {
        TaskContainer startTask = new TaskContainer(UUID.randomUUID(), UUID.randomUUID(), "testMethod2", "testDecider#0.1", TaskType.DECIDER_START, System.currentTimeMillis(), 5, null, null);

        taskServer.startProcess(startTask);//enqueue task

        //1. Get task to work
        int tasksPolled = pollForTasks(taskServer, startTask.getActorId(), 2).size();
        Assert.assertEquals("Only one task should be polled out", 1, tasksPolled);

        //2. Expire it
        ensureExpiration();//Wait to ensure that task is now expired

        //3. Recover it
        recovery.run();

        //4. Get it on polling again
        int actualRecovered = pollForTasks(taskServer, startTask.getActorId(), 2).size();
        Assert.assertEquals("Only one task should be polled out after recovery", expectedRecovered, actualRecovered);

    }

    //Tries to poll for task for [times] times
    private List<TaskContainer> pollForTasks(TaskServer taskServer, String actorId, int times) {
        List<TaskContainer> result = new ArrayList<TaskContainer>();

        for(int i=0; i<times; i++) {
            TaskContainer polledTask = taskServer.poll(ActorUtils.getActorDefinition(actorId));
            if (polledTask!=null) {
                result.add(polledTask);
            }
        }

        return result;
    }

    private void ensureExpiration() {
        long sleepFor = timeoutUnit.toMillis(timeout);
        try {
            Thread.sleep(sleepFor);
        } catch (InterruptedException e) {
            logger.error("Thread sleep for["+sleepFor+"] interrupted!", e);
        }
    }

}

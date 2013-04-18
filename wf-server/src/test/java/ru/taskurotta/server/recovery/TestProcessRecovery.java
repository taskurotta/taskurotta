package ru.taskurotta.server.recovery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.util.ActorUtils;

public class TestProcessRecovery {

    private static final Logger logger = LoggerFactory.getLogger(TestProcessRecovery.class);

    @Test
    public void testProcessRecovery() {
        TaskServer taskServer = RecoveryFactory.getTaskServer();

        Assert.assertNotNull(RecoveryFactory.getConfigBackend());
        Assert.assertNotNull(RecoveryFactory.getConfigBackend().getActorPreferences());
        Assert.assertNotNull(RecoveryFactory.getConfigBackend().getExpirationPolicies());

        UUID processId = UUID.randomUUID();
        UUID taskUuid1 = UUID.randomUUID();
       // UUID taskUuid2 = UUID.randomUUID();

        TaskContainer startTask = RecoveryFactory.getDeciderTaskContainer(taskUuid1, processId);
        //TaskContainer workerTask = RecoveryFactory.getWorkerTaskContainer(taskUuid2, processId);
        taskServer.startProcess(startTask);

        List<TaskContainer> polledTasks = new ArrayList<TaskContainer>();
        for(int i=0; i<3; i++) {
            TaskContainer polledTask = taskServer.poll(ActorUtils.getActorDefinition(startTask.getActorId()));
            if(polledTask!=null) {
                polledTasks.add(polledTask);
            }
        }
        Assert.assertEquals("Only one task should be polled out", polledTasks.size(), 1);

        //Wait to ensure that task (process) is now expired
        long sleepFor = RecoveryFactory.timeoutUnit.toMillis(RecoveryFactory.timeout);
        try {
            Thread.sleep(sleepFor);
        } catch (InterruptedException e) {
            logger.error("Thread sleep for["+sleepFor+"] interrupted!", e);
        }

        ProcessBackendEnqueueTaskRecovery recovery = RecoveryFactory.getProcessRecoveryProcess(TimeoutType.PROCESS_START_TO_CLOSE);
        recovery.run();

        //Process recovery

        //Task should be enqueued again
//        List<TaskContainer> recoveredTasks = new ArrayList<TaskContainer>();
//        for(int i=0; i<3; i++) {
//            TaskContainer polledTask = taskServer.poll(ActorUtils.getActorDefinition(startTask.getActorId()));
//            if(polledTask!=null) {
//                recoveredTasks.add(polledTask);
//            }
//        }
       // Assert.assertEquals("Only one task should be polled out after recovery", recoveredTasks.size(), 1);


    }


}

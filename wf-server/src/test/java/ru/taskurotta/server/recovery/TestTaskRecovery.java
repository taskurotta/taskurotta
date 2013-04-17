package ru.taskurotta.server.recovery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Test;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.util.ActorUtils;

public class TestTaskRecovery {

    @Test
    public void testTaskRecovery() {
        TaskServer taskServer = RecoveryFactory.getTaskServer();

        Assert.assertNotNull(RecoveryFactory.getConfigBackend());
        Assert.assertNotNull(RecoveryFactory.getConfigBackend().getActorPreferences());
        Assert.assertNotNull(RecoveryFactory.getConfigBackend().getExpirationPolicies());

        TaskBackendEnqueueTaskRecovery startToCloseRecovery = RecoveryFactory.getTaskRecoveryProcess(TimeoutType.TASK_START_TO_CLOSE);
        TaskBackendEnqueueTaskRecovery pollToCommitRecovery = RecoveryFactory.getTaskRecoveryProcess(TimeoutType.TASK_POLL_TO_COMMIT);

        TaskContainer startTask = RecoveryFactory.getDeciderTaskContainer(UUID.randomUUID(), UUID.randomUUID());

        taskServer.startProcess(startTask);//enqueue task

        List<TaskContainer> polledTasks = pollForTasks(startTask.getActorId(), 2);
        Assert.assertEquals("Only one task should be polled out", polledTasks.size(), 1);

        RecoveryFactory.ensureExpiration();//Wait to ensure that task is now expired
        startToCloseRecovery.run();

        //Task should be enqueued again
        List<TaskContainer> recoveredTasks = pollForTasks(startTask.getActorId(), 2);
        Assert.assertEquals("Only one task should be polled out after recovery", recoveredTasks.size(), 1);

        taskServer.startProcess(startTask);//enqueue task
        List<TaskContainer> polledTasks2 = pollForTasks(startTask.getActorId(), 2);
        Assert.assertEquals("Only one task should be polled out", polledTasks2.size(), 1);

        RecoveryFactory.ensureExpiration();//Wait to ensure that task is now expired
        pollToCommitRecovery.run();

        //Task should not be recovered: timeout type mismatch
        List<TaskContainer> recoveredTasks2 = pollForTasks(startTask.getActorId(), 2);
        Assert.assertEquals("Tasks with unchecked timeout should not be recovered", recoveredTasks2.size(), 0);


    }

    private List<TaskContainer> pollForTasks(String actorId, int times) {
        List<TaskContainer> result = new ArrayList<TaskContainer>();
        for(int i=0; i<times; i++) {
            TaskContainer polledTask = RecoveryFactory.getTaskServer().poll(ActorUtils.getActorDefinition(actorId));
            if(polledTask!=null) {
                result.add(polledTask);
            }
        }
        return result;
    }

}

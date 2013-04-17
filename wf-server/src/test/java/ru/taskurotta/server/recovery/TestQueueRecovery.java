package ru.taskurotta.server.recovery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Test;

import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.TaskServer;

public class TestQueueRecovery {

    @Test
    public void testProcessRecovery() {
        TaskServer taskServer = RecoveryFactory.getTaskServer();

        Assert.assertNotNull(RecoveryFactory.getConfigBackend());
        Assert.assertNotNull(RecoveryFactory.getConfigBackend().getActorPreferences());
        Assert.assertNotNull(RecoveryFactory.getConfigBackend().getExpirationPolicies());

        QueueBackendEnqueueTaskRecovery pollToCommitRecovery = RecoveryFactory.getQueueRecoveryProcess(TimeoutType.TASK_POLL_TO_COMMIT);

        TaskContainer startTask = RecoveryFactory.getDeciderTaskContainer(UUID.randomUUID(), UUID.randomUUID());

        /* <SHOULD RECOVER> */
        taskServer.startProcess(startTask);//Enqueue task

        //poll for task
        List<UUID> polledTasks = pollForTasks(startTask.getActorId(), 2, false);//Without commit!
        Assert.assertEquals("Only one task should be polled out", polledTasks.size(), 1);

        RecoveryFactory.ensureExpiration();//Wait to ensure that task is now expired
        pollToCommitRecovery.run();//try to recover expired task

        //Task should be enqueued again
        List<UUID> recoveredTasks = pollForTasks(startTask.getActorId(), 2, true);
        Assert.assertEquals("Task should be polled out after recovery", recoveredTasks.size(), 1);
        /*</SHOULD RECOVER> */

        /* <SHOULD NOT RECOVER> */
        taskServer.startProcess(startTask);//Enqueue task

        List<UUID> polledTasks2 = pollForTasks(startTask.getActorId(), 2, true);//With commit!
        Assert.assertEquals("Only one task should be polled out", polledTasks2.size(), 1);

        RecoveryFactory.ensureExpiration();//Wait to ensure that task is now expired
        pollToCommitRecovery.run();//try to recover expired task

        List<UUID> recoveredTasks2 = pollForTasks(startTask.getActorId(), 2, true);
        Assert.assertEquals("There should be no recovered tasks: all checkpoints have been removed", recoveredTasks2.size(), 0);
        /* </SHOULD NOT RECOVER> */

    }

    private List<UUID> pollForTasks(String actorId, int times, boolean commiting) {
        List<UUID> result = new ArrayList<UUID>();
        QueueBackend queueBackend  = RecoveryFactory.getQueueBackend();
        for(int i=0; i<times; i++) {
            UUID polledTask = queueBackend.poll(actorId, null);
            if(polledTask!=null) {
                result.add(polledTask);
                if(commiting) {
                    queueBackend.pollCommit(actorId, polledTask);
                }
            }
        }
        return result;
    }


}

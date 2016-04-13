package ru.taskurotta.server.quorum;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 */
public class QuorumTaskServerProxy implements TaskServer {

    private TaskServer original;
    private ClusterQuorum clusterQuorum;

    public QuorumTaskServerProxy(TaskServer original, ClusterQuorum clusterQuorum) {
        this.original = original;
        this.clusterQuorum = clusterQuorum;
    }

    @Override
    public void startProcess(TaskContainer task) {
        int needToQuorum = clusterQuorum.needToQuorum();
        if (needToQuorum <= 0) {
            original.startProcess(task);
            return;
        }

        throwOutOfQuorumException(needToQuorum);
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        int needToQuorum = clusterQuorum.needToQuorum();
        if (needToQuorum <= 0) {
            return original.poll(actorDefinition);
        }

        throwOutOfQuorumException(needToQuorum);
        return null;
    }

    @Override
    public void release(DecisionContainer taskResult) {
        int needToQuorum = clusterQuorum.needToQuorum();
        if (needToQuorum <= 0) {
            original.release(taskResult);
            return;
        }

        throwOutOfQuorumException(needToQuorum);
    }

    @Override
    public void updateTaskTimeout(UUID taskId, UUID processId, long timeout) {
        int needToQuorum = clusterQuorum.needToQuorum();
        if (needToQuorum <= 0) {
            original.updateTaskTimeout(taskId, processId, timeout);
            return;
        }

        throwOutOfQuorumException(needToQuorum);
    }

    private void throwOutOfQuorumException(int needToQuorum) {
        throw new OutOfQuorumException("need to quorum " + needToQuorum);
    }

}

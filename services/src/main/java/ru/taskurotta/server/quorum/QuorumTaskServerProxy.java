package ru.taskurotta.server.quorum;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

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
        if (clusterQuorum.isPresent()) {
            original.startProcess(task);
            return;
        }

        throwOutOfQuorumException();
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        if (clusterQuorum.isPresent()) {
            return original.poll(actorDefinition);
        }

        throwOutOfQuorumException();
        return null;
    }

    @Override
    public void release(DecisionContainer taskResult) {
        if (clusterQuorum.isPresent()) {
            original.release(taskResult);
            return;
        }

        throwOutOfQuorumException();
    }

    private void throwOutOfQuorumException() {
        throw new OutOfQuorumException();
    }

}

package ru.taskurotta.assemble;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 */
public class ProxyTaskServer implements TaskServer {

    private final TaskServer target;

    public ProxyTaskServer(TaskServer target) {
        this.target = target;
    }

    @Override
    public void startProcess(TaskContainer task) {
        target.startProcess(task);
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        return target.poll(actorDefinition);
    }

    @Override
    public void release(DecisionContainer taskResult) {
        target.release(taskResult);
    }

    @Override
    public void updateTaskTimeout(UUID taskId, UUID processId, long timeout) {
        target.updateTaskTimeout(taskId, processId, timeout);
    }
}

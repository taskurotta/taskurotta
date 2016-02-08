package ru.taskurotta.assemble;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

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
}

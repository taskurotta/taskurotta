package ru.taskurotta.assemple;

import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 12/3/13
 * Time: 9:26 PM
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

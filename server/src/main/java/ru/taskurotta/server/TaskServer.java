package ru.taskurotta.server;


import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

/**
 */
public interface TaskServer {

    /**
     * @param task
     */
    public void startProcess(TaskContainer task);


    /**
     * @param actorDefinition
     * @return
     */
    public TaskContainer poll(ActorDefinition actorDefinition);


    /**
     * @param taskResult
     */
    public void release(DecisionContainer taskResult);


}

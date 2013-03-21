package ru.taskurotta.server;


import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 11:49 AM
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
    public TaskContainer pull(ActorDefinition actorDefinition);


    /**
     * @param taskResult
     */
    public void release(DecisionContainer taskResult);

}

package ru.taskurotta.server;


import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import java.util.UUID;

/**
 */
public interface TaskServer {

    String FIELD_QUEUE_OWNER = TaskServer.class.getName() + "#queue-owner";
    String FIELD_PROCESS_OWNER = TaskServer.class.getName() + "#process-owner";

    ThreadLocal<String> queueOwner = new ThreadLocal<String>();
    ThreadLocal<String> processOwner = new ThreadLocal<String>();


    /**
     * @param task
     */
    void startProcess(TaskContainer task);


    /**
     * @param actorDefinition
     * @return
     */
    TaskContainer poll(ActorDefinition actorDefinition);


    /**
     * @param taskResult
     */
    void release(DecisionContainer taskResult);


    /**
     * Set new task timeout value
     *
     * @param taskId task unique id
     * @param processId process unique id
     * @param timeout in milliseconds
     */
    void updateTaskTimeout(UUID taskId, UUID processId, long timeout);
}

package ru.taskurotta.console.retriever;

import ru.taskurotta.transport.model.TaskContainer;

import java.util.List;
import java.util.UUID;

/**
 * Task information retriever. Provides info about given tasks.
 * User: dimadin
 * Date: 17.05.13 16:05
 */
public interface TaskInfoRetriever {

    public TaskContainer getTask(UUID taskId);

    public List<TaskContainer> getProcessTasks(UUID processId);

}

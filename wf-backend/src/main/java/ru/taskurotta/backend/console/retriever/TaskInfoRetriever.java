package ru.taskurotta.backend.console.retriever;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * Task information retriever. Provides info about given tasks.
 * User: dimadin
 * Date: 17.05.13 16:05
 */
public interface TaskInfoRetriever {

    public TaskContainer getTask(UUID taskId);

    public List<TaskContainer> getProcessTasks(UUID processId);

    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    public DecisionContainer getTaskDecision(UUID taskId);

    public List<TaskContainer> getRepeatedTasks(int iterationCount);

}

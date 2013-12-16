package ru.taskurotta.service.console.retriever;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Task information retriever. Provides info about given tasks.
 * User: dimadin
 * Date: 17.05.13 16:05
 */
public interface TaskInfoRetriever {

    public TaskContainer getTask(UUID taskId, UUID processId);

    public List<TaskContainer> findTasks(TaskSearchCommand command);

    public Collection<TaskContainer> getProcessTasks(Collection<UUID> processTaskIds, UUID processId);

    public GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    public DecisionContainer getDecision(UUID taskId, UUID processId);

    public Collection<TaskContainer> getRepeatedTasks(int iterationCount);

}

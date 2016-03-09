package ru.taskurotta.service.console.retriever;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.command.TaskSearchCommand;
import ru.taskurotta.service.storage.TaskUID;
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

    TaskContainer getTask(UUID taskId, UUID processId);

    List<TaskContainer> findTasks(TaskSearchCommand command);

    Collection<TaskContainer> getProcessTasks(Collection<UUID> processTaskIds, UUID processId);

    GenericPage<TaskContainer> listTasks(int pageNumber, int pageSize);

    DecisionContainer getDecisionContainer(UUID taskId, UUID processId);

    Collection<TaskContainer> getRepeatedTasks(int iterationCount);

    List<TaskUID> getInProgressTasks(int size);

}

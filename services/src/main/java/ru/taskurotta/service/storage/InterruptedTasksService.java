package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.GroupCommand;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.console.model.TaskIdentifier;
import ru.taskurotta.service.console.model.TasksGroupVO;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created on 19.03.2015.
 */
public interface InterruptedTasksService {

    int MESSAGE_MAX_LENGTH = 500;

    void save(InterruptedTask brokenProcess, String fullMessage, String stackTrace);

    Collection<InterruptedTask> find(SearchCommand searchCommand);

    Collection<InterruptedTask> findAll();

    void delete(UUID processId, UUID taskId);

    String getFullMessage(UUID processId, UUID taskId);

    String getStackTrace(UUID processId, UUID taskId);

    List<TasksGroupVO> getGroupList(GroupCommand command);

    Collection<TaskIdentifier> getTaskIdentifiers(GroupCommand command);

    Set<UUID> getProcessIds(GroupCommand command);

    long deleteTasksForProcess(UUID processId);

    boolean isKnown(InterruptedTask task);

    int getKnowInterruptedTasksCount();
}

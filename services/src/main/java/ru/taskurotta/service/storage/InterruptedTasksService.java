package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;

import java.util.Collection;
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

}

package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;

import java.util.Collection;
import java.util.UUID;

/**
 * Created on 19.03.2015.
 */
public interface InterruptedTasksService {

    void save(InterruptedTask brokenProcess);

    Collection<InterruptedTask> find(SearchCommand searchCommand);

    Collection<InterruptedTask> findAll();

    void delete(UUID processId, UUID taskId);

//    void restart(UUID processId, UUID taskId);


}

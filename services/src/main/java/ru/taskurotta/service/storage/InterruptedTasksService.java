package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;

import java.util.Collection;
import java.util.UUID;

/**
 * Created on 19.03.2015.
 */
public interface InterruptedTasksService {

    public void save(InterruptedTask brokenProcess);

    public Collection<InterruptedTask> find(SearchCommand searchCommand);

    public Collection<InterruptedTask> findAll();

    public void delete(UUID processId, UUID taskId);

}

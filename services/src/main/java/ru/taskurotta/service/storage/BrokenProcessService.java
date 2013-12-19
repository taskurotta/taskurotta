package ru.taskurotta.service.storage;

import ru.taskurotta.service.console.model.BrokenProcess;
import ru.taskurotta.service.console.model.SearchCommand;

import java.util.Collection;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:24
 */

public interface BrokenProcessService {

    public void save(BrokenProcess brokenProcess);

    public Collection<BrokenProcess> find(SearchCommand searchCommand);

    public Collection<BrokenProcess> findAll();

    public void delete(UUID processId);

    public void deleteCollection(Collection<UUID> processIds);
}

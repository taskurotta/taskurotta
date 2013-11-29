package ru.taskurotta.backend.process;

import java.util.Collection;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:24
 */

public interface BrokenProcessBackend {

    public void save(BrokenProcessVO brokenProcessVO);

    public Collection<BrokenProcessVO> find(SearchCommand searchCommand);

    public Collection<BrokenProcessVO> findAll();

    public void delete(UUID processId);

    public void deleteCollection(Collection<UUID> processIds);
}

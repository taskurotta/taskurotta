package ru.taskurotta.backend.process;

import java.util.Collection;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:24
 */

public interface BrokenProcessBackend {

    public void save(BrokenProcessVO brokenProcessVO);

    public Collection<BrokenProcessVO> find(SearchCommand searchCommand);

    public Collection<BrokenProcessVO> findAll();
}

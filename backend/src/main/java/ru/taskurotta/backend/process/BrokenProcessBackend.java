package ru.taskurotta.backend.process;

import java.util.List;

/**
 * User: stukushin
 * Date: 11.10.13
 * Time: 18:24
 */

public interface BrokenProcessBackend {

    public void save(BrokenProcessVO brokenProcessVO);

    public List<BrokenProcessVO> find(SearchObject searchObject);
}

package ru.taskurotta.service.recovery;

import java.util.Collection;
import java.util.UUID;

/**
 * Date: 13.01.14 11:08
 */
public interface IncompleteProcessDao {

    Collection<UUID> findProcesses(long timeBefore, int limit);

}

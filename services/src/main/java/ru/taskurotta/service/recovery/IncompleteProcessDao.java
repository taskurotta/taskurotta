package ru.taskurotta.service.recovery;

/**
 * Date: 13.01.14 11:08
 */
public interface IncompleteProcessDao {

    IncompleteProcessesCursor findProcesses(long timeBefore, int limit);

}

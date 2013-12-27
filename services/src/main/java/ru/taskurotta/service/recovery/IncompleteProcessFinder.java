package ru.taskurotta.service.recovery;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 18.12.13
 * Time: 14:48
 */
public interface IncompleteProcessFinder {

    public void toRecovery(UUID processId);

}

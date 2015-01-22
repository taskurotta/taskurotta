package ru.taskurotta.service.recovery;

import java.util.Collection;
import java.util.UUID;

/**
 */
public interface IncompleteProcessesCursor {

    public Collection<UUID> getNext();
}

package ru.taskurotta.service.recovery;

import java.io.Closeable;
import java.util.Collection;
import java.util.UUID;

/**
 */
public interface IncompleteProcessesCursor extends Closeable{

    public Collection<UUID> getNext();
}

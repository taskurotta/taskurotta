package ru.taskurotta.service.gc;

import java.util.UUID;

public interface GarbageCollectorService {

    void collect(UUID processId, long timeout);

    int getCurrentSize();

}

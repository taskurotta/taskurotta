package ru.taskurotta.backend.console.retriever;

import java.util.Collection;
import java.util.UUID;

/**
 * User: romario
 * Date: 8/2/13
 * Time: 11:04 AM
 */
public interface GraphInfoRetriever {

    public Collection<UUID> getProcessTasks(UUID processId);

}

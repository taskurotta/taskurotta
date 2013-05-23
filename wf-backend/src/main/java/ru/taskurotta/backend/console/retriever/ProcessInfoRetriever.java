package ru.taskurotta.backend.console.retriever;

import ru.taskurotta.backend.console.model.ProcessVO;

import java.util.UUID;

/**
 * Process information retriever. Provides info about processes, such as number of active processes, their id's, start times and such.
 * User: dimadin
 * Date: 17.05.13 16:05
 */
public interface ProcessInfoRetriever {

    public ProcessVO getProcess(UUID processUUID);

}

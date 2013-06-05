package ru.taskurotta.backend.console.retriever;

import java.util.List;
import java.util.UUID;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;

/**
 * Process information retriever. Provides info about processes, such as number of active processes, their id's, start times and such.
 * User: dimadin
 * Date: 17.05.13 16:05
 */
public interface ProcessInfoRetriever {

    public static final String SEARCH_BY_ID = "process_id";
    public static final String SEARCH_BY_CUSTOM_ID = "custom_id";

    public ProcessVO getProcess(UUID processUUID);

    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize);

    public List<ProcessVO> findProcesses(String type, String id);
}

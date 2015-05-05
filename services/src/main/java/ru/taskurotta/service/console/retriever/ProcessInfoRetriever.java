package ru.taskurotta.service.console.retriever;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;

import java.util.List;
import java.util.UUID;

/**
 * Process information retriever. Provides info about processes, such as number of active processes, their id's, start times and such.
 * User: dimadin
 * Date: 17.05.13 16:05
 */
public interface ProcessInfoRetriever {

    Process getProcess(UUID processUUID);

    GenericPage<Process> listProcesses(int pageNumber, int pageSize, int status, String typeFilter);

    List<Process> findProcesses(ProcessSearchCommand command);

    int getFinishedCount(String customId);

    int getBrokenProcessCount();
}

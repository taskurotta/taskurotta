package ru.taskurotta.service.console.retriever;

/**
 * Created on 15.05.2015.
 */
public interface StatInfoRetriever {

    String getHazelcastStats();

    String getNodeStats();

    int getFinishedProcessesCounter();

}

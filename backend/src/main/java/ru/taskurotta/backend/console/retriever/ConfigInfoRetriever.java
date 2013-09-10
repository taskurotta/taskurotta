package ru.taskurotta.backend.console.retriever;

import java.util.Collection;

/**
 * User: stukushin
 * Date: 22.07.13
 * Time: 13:59
 */
public interface ConfigInfoRetriever {

    public void blockActor(String actorId);

    public void unblockActor(String actorId);

    public Collection<String> getActorIdList();
}

package ru.taskurotta.backend.console.retriever;

import ru.taskurotta.backend.console.model.GenericPage;

/**
 * User: stukushin
 * Date: 22.07.13
 * Time: 13:59
 */
public interface ConfigInfoRetriever {

    public void blockActor(String actorId);

    public void unblockActor(String actorId);

    public GenericPage<String> getActorIdList(int pageNum, int pageSize);

}

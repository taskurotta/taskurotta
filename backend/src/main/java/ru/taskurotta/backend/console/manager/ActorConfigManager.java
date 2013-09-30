package ru.taskurotta.backend.console.manager;

import ru.taskurotta.backend.console.model.ActorVO;
import ru.taskurotta.backend.console.model.GenericPage;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 27.09.13 17:56
 */
public interface ActorConfigManager {

    public void blockActor(String actorId);

    public void unblockActor(String actorId);

    public boolean isBlocked(String actorId);

    public GenericPage<ActorVO> getActorList(int pageNum, int pageSize);

}

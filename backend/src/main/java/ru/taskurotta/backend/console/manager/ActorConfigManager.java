package ru.taskurotta.backend.console.manager;

import ru.taskurotta.backend.console.model.ActorVO;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.statistics.QueueBalanceVO;

/**
 * Interface for the console manager providing
 * information on actors and handling their actions
 * User: dimadin
 * Date: 27.09.13 17:56
 */
public interface ActorConfigManager {

//    public void blockActor(String actorId);
//
//    public void unblockActor(String actorId);
//
//    public boolean isActorBlocked(String actorId);

    public GenericPage<ActorVO> getActorList(int pageNum, int pageSize);

    public QueueBalanceVO getQueueState(String actorId);

}

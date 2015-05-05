package ru.taskurotta.service.console.manager;

import ru.taskurotta.service.console.model.ActorVO;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.MetricsStatDataVO;
import ru.taskurotta.service.metrics.model.QueueBalanceVO;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for the console manager providing
 * information on actors and handling their actions
 * User: dimadin
 * Date: 27.09.13 17:56
 */
public interface ActorConfigManager {

    GenericPage<ActorVO> getActorList(int pageNum, int pageSize, String filter);

    QueueBalanceVO getQueueState(String queueName);

    Map<String, Collection<MetricsStatDataVO>> getMetricsData(Collection<String> metrics, Collection<String> actorIds);

}

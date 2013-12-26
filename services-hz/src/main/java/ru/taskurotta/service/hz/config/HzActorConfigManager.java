package ru.taskurotta.service.hz.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.config.model.ActorPreferences;
import ru.taskurotta.service.console.manager.ActorConfigManager;
import ru.taskurotta.service.console.model.ActorVO;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.MetricsStatDataVO;
import ru.taskurotta.service.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.model.QueueBalanceVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of ActorConfigManager for Hazelcast.
 * Addresses hazelcast map for actors preferences
 *
 * Date: 27.09.13 18:03
 */
public class HzActorConfigManager implements ActorConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(HzActorConfigManager.class);
    public static final String ACTOR_CONFIG_EXECUTOR_SERVICE = "actorConfigExecutorService";

    protected String actorConfigName;
    protected HazelcastInstance hzInstance;
    protected MetricsMethodDataRetriever metricsDataRetriever;

    public HzActorConfigManager(HazelcastInstance hzInstance, String actorConfigName) {
        this.hzInstance = hzInstance;
        this.actorConfigName = actorConfigName;
    }

    @Override
    public GenericPage<ActorVO> getActorList(int pageNum, int pageSize) {
        GenericPage<ActorVO> result = null;
        IMap<String, ActorPreferences> actorsPrefs = hzInstance.getMap(actorConfigName);
        List<ActorPreferences> allPreferences = new ArrayList(actorsPrefs.values());

        if (allPreferences!=null && !allPreferences.isEmpty()) {
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(pageSize * pageNum, allPreferences.size());
            List<ActorPreferences> subList = allPreferences.subList(fromIndex, toIndex);
            List<ActorVO> pageItems = new ArrayList<>();
            for (ActorPreferences ap : subList) {
                ActorVO actorVO = new ActorVO();
                actorVO.setActorId(ap.getId());
                actorVO.setBlocked(ap.isBlocked());
                actorVO.setQueueName(ap.getQueueName());
                if (metricsDataRetriever!=null) {
                    actorVO.setLastPoll(metricsDataRetriever.getLastActivityTime(MetricName.POLL.getValue(), ap.getId()));
                    actorVO.setLastRelease(metricsDataRetriever.getLastActivityTime(MetricName.RELEASE.getValue(), ap.getId()));
                }
                pageItems.add(actorVO);
            }
            result = new GenericPage<ActorVO>(pageItems, pageNum, pageSize, allPreferences.size());
        }

        return result;
    }

    @Override
    public QueueBalanceVO getQueueState(final String actorId) {
        IExecutorService executorService = hzInstance.getExecutorService(ACTOR_CONFIG_EXECUTOR_SERVICE);

        Map <Member, Future <QueueBalanceVO>> futures = executorService.submitToMembers(new ComputeQueueBalanceTask(actorId), hzInstance.getCluster().getMembers());

        QueueBalanceVO result = new QueueBalanceVO();
        for (Future<QueueBalanceVO> future : futures.values()) {
            try {
                QueueBalanceVO nodeVal = future.get(5, TimeUnit.SECONDS);
                result = HzActorConfigUtils.sumQueueStates(result, nodeVal);
                if (nodeVal != null) {
                    result.setNodes(result.getNodes()+1);
                }
            } catch (Exception e) {
                logger.error("Cannot get queue state value for actorId["+actorId+"]", e);
            }
        }
        logger.debug("Cluster wide queue state getted is [{}]", result);
        return result;
    }

    @Override
    public Map<String, Collection<MetricsStatDataVO>> getMetricsData(Collection<String> metrics, Collection<String> actorIds) {
        IExecutorService executorService = hzInstance.getExecutorService(ACTOR_CONFIG_EXECUTOR_SERVICE);
        Map<String, Collection<MetricsStatDataVO>> result = new HashMap<>();

        Map <Member, Future <Collection<MetricsStatDataVO>>> futures = executorService.submitToMembers(new ComputeMetricsStatDataTask(metrics, actorIds), hzInstance.getCluster().getMembers());
        if (futures!=null && !futures.isEmpty()) {
            for (Map.Entry<Member, Future <Collection<MetricsStatDataVO>>> entry : futures.entrySet()) {
                try {
                    Future <Collection<MetricsStatDataVO>> future = entry.getValue();
                    result.put(entry.getKey().toString(), future.get(5, TimeUnit.SECONDS));
                } catch (Exception e) {
                    logger.error("Cannot get metrics stat data for actorIds["+actorIds+"], metrics["+metrics+"]", e);
                }
            }
        }

        return result;
    }

    public void setMetricsDataRetriever(MetricsMethodDataRetriever metricsDataRetriever) {
        this.metricsDataRetriever = metricsDataRetriever;
    }

}

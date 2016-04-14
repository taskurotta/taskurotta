package ru.taskurotta.service.hz.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.config.model.ActorPreferences;
import ru.taskurotta.service.console.manager.ActorConfigManager;
import ru.taskurotta.service.console.model.ActorExtVO;
import ru.taskurotta.service.console.model.ActorFullVO;
import ru.taskurotta.service.console.model.ActorState;
import ru.taskurotta.service.console.model.ActorVO;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.MetricsStatDataVO;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.console.retriever.TaskInfoRetriever;
import ru.taskurotta.service.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.RateUtils;
import ru.taskurotta.service.metrics.handler.DatasetSummary;
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
 * <p>
 * Date: 27.09.13 18:03
 */
public class HzActorConfigManager implements ActorConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(HzActorConfigManager.class);
    public static final String ACTOR_CONFIG_EXECUTOR_SERVICE = "actorConfigExecutorService";

    protected String actorConfigName;
    protected HazelcastInstance hzInstance;
    protected MetricsMethodDataRetriever metricsDataRetriever;
    protected QueueInfoRetriever queueInfoRetriever;
    protected TaskInfoRetriever taskInfoRetriever;
    private long pollTimeout;

    public HzActorConfigManager(HazelcastInstance hzInstance, String actorConfigName,
                                QueueInfoRetriever queueInfoRetriever, long pollTimeout,
                                MetricsMethodDataRetriever metricsDataRetriever,
                                TaskInfoRetriever taskInfoRetriever) {
        this.hzInstance = hzInstance;
        this.actorConfigName = actorConfigName;
        this.queueInfoRetriever = queueInfoRetriever;
        this.pollTimeout = pollTimeout;
        this.metricsDataRetriever = metricsDataRetriever;
        this.taskInfoRetriever = taskInfoRetriever;
    }

    @Override
    public GenericPage<ActorVO> getActorList(int pageNum, int pageSize, String filter) {
        GenericPage<ActorVO> result = null;
        IMap<String, ActorPreferences> actorsPrefs = hzInstance.getMap(actorConfigName);
        List<ActorPreferences> allPreferences;
        if (filter != null && !filter.trim().isEmpty()) {
            allPreferences = new ArrayList(actorsPrefs.values(Predicates.like("id", filter + "%")));
        } else {
            allPreferences = new ArrayList(actorsPrefs.values());
        }

        if (allPreferences != null && !allPreferences.isEmpty()) {
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(pageSize * pageNum, allPreferences.size());
            List<ActorPreferences> subList = allPreferences.subList(fromIndex, toIndex);
            List<ActorVO> pageItems = new ArrayList<>();
            for (ActorPreferences ap : subList) {
                pageItems.add(createActorVO(ap));
            }
            result = new GenericPage<ActorVO>(pageItems, pageNum, pageSize, allPreferences.size());
        }

        return result;
    }

    @Override
    public QueueBalanceVO getQueueState(final String queueName) {
        IExecutorService executorService = hzInstance.getExecutorService(ACTOR_CONFIG_EXECUTOR_SERVICE);

        Map<Member, Future<QueueBalanceVO>> futures = executorService.submitToMembers(new ComputeQueueBalanceTask(queueName), hzInstance.getCluster().getMembers());

        QueueBalanceVO result = new QueueBalanceVO();
        for (Future<QueueBalanceVO> future : futures.values()) {
            try {
                QueueBalanceVO nodeVal = future.get(5, TimeUnit.SECONDS);
                result = HzActorConfigUtils.sumQueueStates(result, nodeVal);
                if (nodeVal != null) {
                    result.setNodes(result.getNodes() + 1);
                }
            } catch (Exception e) {
                logger.error("Cannot get queue state value for queue[" + queueName + "]", e);
            }
        }
        logger.debug("Cluster wide queue state got is [{}]", result);
        return result;
    }

    @Override
    public Map<String, Collection<MetricsStatDataVO>> getMetricsData(Collection<String> metrics, Collection<String> actorIds) {
        IExecutorService executorService = hzInstance.getExecutorService(ACTOR_CONFIG_EXECUTOR_SERVICE);
        Map<String, Collection<MetricsStatDataVO>> result = new HashMap<>();

        Map<Member, Future<Collection<MetricsStatDataVO>>> futures = executorService.submitToMembers(new ComputeMetricsStatDataTask(metrics, actorIds), hzInstance.getCluster().getMembers());
        if (futures != null && !futures.isEmpty()) {
            for (Map.Entry<Member, Future<Collection<MetricsStatDataVO>>> entry : futures.entrySet()) {
                try {
                    Future<Collection<MetricsStatDataVO>> future = entry.getValue();
                    result.put(entry.getKey().toString(), future.get(5, TimeUnit.SECONDS));
                } catch (Exception e) {
                    logger.error("Cannot get metrics stat data for actorIds[" + actorIds + "], metrics[" + metrics + "]", e);
                }
            }
        }

        return result;
    }

    public Collection<MetricsStatDataVO> getAllMetrics(String actorId) {
        IExecutorService executorService = hzInstance.getExecutorService(ACTOR_CONFIG_EXECUTOR_SERVICE);
        Collection<MetricsStatDataVO> result = new ArrayList<>();

        Map<Member, Future<Collection<MetricsStatDataVO>>> futures = executorService.submitToMembers(
                new AllActorMetricsTask(actorId), hzInstance.getCluster().getMembers());
        for (Map.Entry<Member, Future<Collection<MetricsStatDataVO>>> entry : futures.entrySet()) {
            try {
                Future<Collection<MetricsStatDataVO>> future = entry.getValue();
                result.addAll(future.get(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                logger.error("Cannot get all metrics stat data for actorId[" + actorId + "]", e);
            }
        }

        return result;
    }

    @Override
    public ActorVO getActorVo(String actorId) {
        IMap<String, ActorPreferences> actorsConfigs = hzInstance.getMap(actorConfigName);
        ActorPreferences actorPreferences = actorsConfigs.get(actorId);
        return createActorVO(actorPreferences);
    }

    @Override
    public ActorExtVO getActorExtVo(String actorId) {

        ActorExtVO extActor = new ActorExtVO(getActorVo(actorId));
        QueueBalanceVO queueBalanceVO = getQueueState(actorId);
        extActor.setQueueState(queueBalanceVO);

        if (queueBalanceVO != null) {
            extActor.setDayRate(RateUtils.getOverallRate(queueBalanceVO.getTotalInDay(),
                    queueBalanceVO.getInDayPeriod(), queueBalanceVO.getTotalOutDay(),
                    queueBalanceVO.getOutDayPeriod()));
            extActor.setHourRate(RateUtils.getOverallRate(queueBalanceVO.getTotalInHour(),
                    queueBalanceVO.getInHourPeriod(), queueBalanceVO.getTotalOutHour(),
                    queueBalanceVO.getOutHourPeriod()));
        }

        return extActor;
    }

    @Override
    public ActorFullVO getActorFullVo(String actorId) {

        ActorFullVO actorFullVO = new ActorFullVO();
        actorFullVO.setId(actorId);

        IMap<String, ActorPreferences> actorsConfigs = hzInstance.getMap(actorConfigName);
        ActorPreferences actorPreferences = actorsConfigs.get(actorId);

        if (actorPreferences == null) {
            return null;
        }

        ActorState actorState = ActorState.ACTIVE;
        if (actorPreferences.isBlocked()) {
            actorState = ActorState.BLOCKED;
        }

        // queue
        // queue size, delay queue size, wait queue time, effective time

        actorFullVO.setQueueSize(queueInfoRetriever.getQueueSize(actorId));
        actorFullVO.setQueueDelaySize(queueInfoRetriever.getQueueDelaySize(actorId));
        actorFullVO.setLastPolledTaskEnqueueTime(queueInfoRetriever.getLastPolledTaskEnqueueTime(actorId));
        // {{(endTime - startTime) | date: 'H:mm:ss': 'UTC'}}
        //Date.now().

        // all metrics data

        Map<String, DatasetSummary> metrics = new HashMap<>();
        addMetricsDatasetSummary(metrics, MetricName.START_PROCESS, actorId);
        addMetricsDatasetSummary(metrics, MetricName.POLL, actorId);
        addMetricsDatasetSummary(metrics, MetricName.SUCCESSFUL_POLL, actorId);
        addMetricsDatasetSummary(metrics, MetricName.RELEASE, actorId);
        addMetricsDatasetSummary(metrics, MetricName.EXECUTION_TIME, actorId);
        addMetricsDatasetSummary(metrics, MetricName.ERROR_DECISION, actorId);
        addMetricsDatasetSummary(metrics, MetricName.ENQUEUE, actorId);
        // todo: this is a Number metric. We should use NumberDataHandler
//        NumberDataHandler ndh = NumberDataHandler.getInstance();
//        addMetricsDatasetSummary(metrics, MetricName.QUEUE_SIZE, actorId);

        actorFullVO.setMetrics(metrics);

        // todo: add list of in progress tasks

        // set absent state
        if (actorState != ActorState.BLOCKED &&
                isActorInactive(actorId, metrics.get(MetricName.POLL.getValue()).getLastTime())) {
            actorState = ActorState.INACTIVE;
        }

        actorFullVO.setState(actorState);
        actorFullVO.setCurrentTimeMillis(System.currentTimeMillis());

        return actorFullVO;
    }

    private void addMetricsDatasetSummary(Map<String, DatasetSummary> metrics, MetricName metricName,
                                          String actorId) {
        metrics.put(metricName.getValue(),
                metricsDataRetriever.getDatasetSummary(metricName.getValue(), actorId));
    }

    private boolean isActorInactive(String actorId, long lastActivity) {
        if (System.currentTimeMillis() - lastActivity > pollTimeout) {
            return true;
        }

        return false;
    }


    private ActorVO createActorVO(ActorPreferences actorPreferences) {
        ActorVO actorVO = new ActorVO();
        actorVO.setId(actorPreferences.getId());
        actorVO.setBlocked(actorPreferences.isBlocked());
        actorVO.setQueueName(actorPreferences.getQueueName());
        if (metricsDataRetriever != null) {
            actorVO.setLastPoll(metricsDataRetriever.getLastActivityTime(MetricName.POLL.getValue(), actorPreferences.getId()));
            actorVO.setLastRelease(metricsDataRetriever.getLastActivityTime(MetricName.RELEASE.getValue(), actorPreferences.getId()));
        }
        return actorVO;
    }

}

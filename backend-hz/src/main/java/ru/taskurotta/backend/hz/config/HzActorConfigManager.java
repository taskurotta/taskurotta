package ru.taskurotta.backend.hz.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.backend.console.manager.ActorConfigManager;
import ru.taskurotta.backend.console.model.ActorVO;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.MetricsStatDataVO;
import ru.taskurotta.backend.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.backend.statistics.QueueBalanceVO;
import ru.taskurotta.server.MetricName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of ActorConfigManager for Hazelcast with MongoDB map store
 * Implements ConfigBackend
 * User: dimadin
 * Date: 27.09.13 18:03
 */
public class HzActorConfigManager implements ActorConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(HzActorConfigManager.class);
    public static final String ACTOR_CONFIG_EXECUTOR_SERVICE = "actorConfigExecutorService";

    private MongoTemplate mongoTemplate;
    private String actorConfigName;
    private HazelcastInstance hzInstance;
    private MetricsMethodDataRetriever metricsDataRetriever;

    public HzActorConfigManager(HazelcastInstance hzInstance, MongoTemplate mongoTemplate, String actorConfigName) {
        this.actorConfigName = actorConfigName;
        this.mongoTemplate = mongoTemplate;
        this.hzInstance = hzInstance;
    }

    protected GenericPage<ActorVO> getMongoDbActorsList (int pageNum, int pageSize) {
        List<ActorVO> items = new ArrayList<ActorVO>();
        long total = 0;
        DBCollection dbColl =  mongoTemplate.getCollection(actorConfigName);
        if(dbColl!=null) {
            DBCursor cursor = dbColl.find().skip((pageNum-1)*pageSize).limit(pageSize);
            while (cursor.hasNext()) {
                DBObject value = cursor.next();
                String actorId = (String)value.get("_id");
                boolean isBlocked = (Boolean)value.get("blocked");
                String queueName = (String)value.get("queueName");

                ActorVO actorVO = new ActorVO();
                actorVO.setActorId(actorId);
                actorVO.setBlocked(isBlocked);
                actorVO.setQueueName(queueName);

                if (metricsDataRetriever!=null) {
                    actorVO.setLastPoll(metricsDataRetriever.getLastActivityTime(MetricName.POLL.getValue(), actorId));
                    actorVO.setLastRelease(metricsDataRetriever.getLastActivityTime(MetricName.RELEASE.getValue(), actorId));
                }

                items.add(actorVO);
            }
            total = dbColl.count();
        }
        return new GenericPage<ActorVO>(items, pageNum, pageSize, total);

    }


    @Override
    public GenericPage<ActorVO> getActorList(int pageNum, int pageSize) {

        //Cannot iterate HZMap, it is evicted to MongoDB due to TTL policy. So scan MongoDB directly
        return getMongoDbActorsList(pageNum, pageSize);

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

    @Required
    public void setMetricsDataRetriever(MetricsMethodDataRetriever metricsDataRetriever) {
        this.metricsDataRetriever = metricsDataRetriever;
    }

}

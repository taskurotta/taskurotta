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
import ru.taskurotta.backend.console.retriever.MetricsDataRetriever;
import ru.taskurotta.backend.statistics.QueueBalanceVO;
import ru.taskurotta.server.MetricName;

import java.util.ArrayList;
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
    private MongoTemplate mongoTemplate;
    private String actorConfigName;
    private HazelcastInstance hzInstance;
    private MetricsDataRetriever metricsDataRetriever;

    public HzActorConfigManager(HazelcastInstance hzInstance, MongoTemplate mongoTemplate, String actorConfigName) {
        this.actorConfigName = actorConfigName;
        this.mongoTemplate = mongoTemplate;
        this.hzInstance = hzInstance;
    }

//    protected GenericPage<String> getMongoDbActorIdList (int pageNum, int pageSize) {
//        List<String> items = new ArrayList<String>();
//        long total = 0;
//        DBCollection dbColl =  mongoTemplate.getCollection(actorConfigName);
//        if(dbColl!=null) {
//            DBCursor cursor = dbColl.find().skip((pageNum-1)*pageSize).limit(pageSize);
//            while (cursor.hasNext()) {
//                DBObject value = cursor.next();
//                items.add((String)value.get("_id"));
//            }
//            total = dbColl.count();
//        }
//        return new GenericPage<String>(items, pageNum, pageSize, total);
//
//    }

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
        IExecutorService executorService = hzInstance.getExecutorService("actorConfigExecutorService");

        Map<Member, Future<QueueBalanceVO>> futures = executorService.submitToMembers(new ComputeQueueBalanceTask(actorId), hzInstance.getCluster().getMembers());

        QueueBalanceVO result = new QueueBalanceVO();
        for (Future<QueueBalanceVO> future : futures.values()) {
            try {
                QueueBalanceVO nodeVal = future.get(5, TimeUnit.SECONDS);
                result = sumQueueStates(result, nodeVal);
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

    private static QueueBalanceVO sumQueueStates(QueueBalanceVO to, QueueBalanceVO from) {
        if (from == null) {
            return to;
        } else if (to == null) {
            return from;
        } else {
            to.setTotalOutHour(getSummedValue(to.getTotalOutHour(), from.getTotalOutHour()));
            to.setOutHourPeriod(getMergedPeriod(to.getOutHourPeriod(), from.getOutHourPeriod()));

            to.setTotalOutDay(getSummedValue(to.getTotalOutDay(), from.getTotalOutDay()));
            to.setOutDayPeriod(getMergedPeriod(to.getOutDayPeriod(), from.getOutDayPeriod()));

            to.setTotalInHour(getSummedValue(to.getTotalInHour(), from.getTotalInHour()));
            to.setInHourPeriod(getMergedPeriod(to.getInHourPeriod(), from.getInHourPeriod()));

            to.setTotalInDay(getSummedValue(to.getTotalInDay(), from.getTotalInDay()));
            to.setInDayPeriod(getMergedPeriod(to.getInDayPeriod(), from.getInDayPeriod()));

            return to;
        }
    }

    private static int getSummedValue(int val1, int val2) {
        if (val1 < 0) {
            return val2;
        } else if(val2 < 0) {
            return val1;
        } else {
            return val1 + val2;
        }
    }


    private static long[] getMergedPeriod(long[] val1, long[] val2) {
        long[] result = {-1l, -1l};

        if ((val2[0] < 0) || (val1[0]>0 && val1[0]<val2[0])) {
            result[0] = val1[0];
        } else {
            result[0] = val2[0];
        }

        if ((val2[1] < 0) || (val1[1]>0 && val1[1]>val2[1])) {
            result[1] = val1[1];
        } else {
            result[1] = val2[1];
        }

        return result;
    }

    @Required
    public void setMetricsDataRetriever(MetricsDataRetriever metricsDataRetriever) {
        this.metricsDataRetriever = metricsDataRetriever;
    }

}

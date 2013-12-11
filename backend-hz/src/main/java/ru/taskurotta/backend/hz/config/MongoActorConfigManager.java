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
import ru.taskurotta.backend.statistics.MetricName;
import ru.taskurotta.backend.statistics.QueueBalanceVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of ActorConfigManager for Hazelcast with MongoDB map store.
 * Addresses directly to mongo mapstore for actors list
 *
 * Date: 11.12.13 16:40
 */
public class MongoActorConfigManager extends HzActorConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(MongoActorConfigManager.class);

    private MongoTemplate mongoTemplate;

    public MongoActorConfigManager (HazelcastInstance hzInstance, MongoTemplate mongoTemplate, String actorConfigName) {
        super(hzInstance, actorConfigName);
        this.mongoTemplate = mongoTemplate;
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

}

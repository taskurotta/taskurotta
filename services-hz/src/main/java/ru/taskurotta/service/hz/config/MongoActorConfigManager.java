package ru.taskurotta.service.hz.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.mongodb.MongoDBConverter;
import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.service.console.model.ActorVO;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.metrics.MetricName;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ActorConfigManager for Hazelcast with MongoDB map store.
 * Addresses directly to mongo mapstore for actors list
 *
 * Date: 11.12.13 16:40
 */
public class MongoActorConfigManager extends HzActorConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(MongoActorConfigManager.class);

    private MongoTemplate mongoTemplate;
    private MongoDBConverter converter;

    public MongoActorConfigManager (HazelcastInstance hzInstance, MongoTemplate mongoTemplate, String actorConfigName) {
        super(hzInstance, actorConfigName);
        this.mongoTemplate = mongoTemplate;
        this.converter = new SpringMongoDBConverter(mongoTemplate);
    }

    protected GenericPage<ActorVO> getMongoDbActorsList (int pageNum, int pageSize) {
        List<ActorVO> items = new ArrayList<ActorVO>();
        long total = 0;
        DBCollection dbColl =  mongoTemplate.getCollection(actorConfigName);
        if(dbColl!=null) {
            try (DBCursor cursor = dbColl.find().skip((pageNum-1)*pageSize).limit(pageSize)) {
                while (cursor.hasNext()) {
                    DBObject value = cursor.next();
                    ActorVO actorVO = (ActorVO) converter.toObject(ActorVO.class, value);
                    if (metricsDataRetriever != null) {
                        actorVO.setLastPoll(metricsDataRetriever.getLastActivityTime(MetricName.POLL.getValue(), actorVO.getId()));
                        actorVO.setLastRelease(metricsDataRetriever.getLastActivityTime(MetricName.RELEASE.getValue(), actorVO.getId()));
                    }

                    items.add(actorVO);
                }
                total = dbColl.count();
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("ActorVO page[{}, {}] items are [{}]", pageNum, pageSize, items);
        }
        return new GenericPage<ActorVO>(items, pageNum, pageSize, total);
    }

    @Override
    public GenericPage<ActorVO> getActorList(int pageNum, int pageSize) {
        //Cannot iterate HZMap, it is evicted to MongoDB due to TTL policy. So scan MongoDB directly
        return getMongoDbActorsList(pageNum, pageSize);
    }

}

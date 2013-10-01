package ru.taskurotta.backend.hz.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.backend.config.model.ActorPreferences;
import ru.taskurotta.backend.console.manager.ActorConfigManager;
import ru.taskurotta.backend.console.model.ActorVO;
import ru.taskurotta.backend.console.model.GenericPage;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for Hazelcast with MongoDB map store
 * User: dimadin
 * Date: 27.09.13 18:03
 */
public class HzActorConfigManager implements ActorConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(HzActorConfigManager.class);
    private MongoTemplate mongoTemplate;
    private IMap<String, ActorPreferences> actorConfig;
    private String actorConfigName;

    public HzActorConfigManager(HazelcastInstance hzInstance, MongoTemplate mongoTemplate, String actorConfigName) {
        this.actorConfigName = actorConfigName;
        this.actorConfig = hzInstance.getMap(actorConfigName);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void blockActor(String actorId) {

        ActorPreferences actorPreferences = actorConfig.get(actorId);
        if (actorPreferences == null) {
            actorPreferences = new ActorPreferences();
            actorPreferences.setId(actorId);
        }

        actorPreferences.setBlocked(true);

        actorConfig.set(actorId, actorPreferences);

        logger.debug("Actor ID [{}] have been blocked", actorId);
    }

    @Override
    public void unblockActor(String actorId) {

        ActorPreferences actorPreferences = actorConfig.get(actorId);

        if (actorPreferences == null) {
            return;
        }

        actorPreferences.setBlocked(false);
        actorConfig.set(actorId, actorPreferences);

        logger.debug("Actor ID [{}] have been unblocked", actorId);
    }

    @Override
    public boolean isBlocked(String actorId) {
        ActorPreferences ap = actorConfig.get(actorId);
        return ap!=null? ap.isBlocked(): false;
    }

    public GenericPage<String> getMongoDbActorIdList (int pageNum, int pageSize) {
        List<String> items = new ArrayList<String>();
        long total = 0;
        DBCollection dbColl =  mongoTemplate.getCollection(actorConfigName);
        if(dbColl!=null) {
            DBCursor cursor = dbColl.find().skip((pageNum-1)*pageSize).limit(pageSize);
            while (cursor.hasNext()) {
                DBObject value = cursor.next();
                items.add((String)value.get("_id"));
            }
            total = dbColl.count();
        }
        return new GenericPage<String>(items, pageNum, pageSize, total);

    }

    @Override
    public GenericPage<ActorVO> getActorList(int pageNum, int pageSize) {
        GenericPage<String> actorIdPage = getMongoDbActorIdList(pageNum, pageSize);
        List<ActorVO> items = null;
        if (actorIdPage.getItems()!=null && !actorIdPage.getItems().isEmpty()) {
            items = new ArrayList<>();
            for (String actorId: actorIdPage.getItems()) {
                ActorVO actorVO = new ActorVO();
                actorVO.setActorId(actorId);
                actorVO.setBlocked(isBlocked(actorId));
                actorVO.setQueueName(actorId);
                items.add(actorVO);
            }

        }
        return new GenericPage<ActorVO>(items, pageNum, pageSize, actorIdPage.getTotalCount());
    }


}

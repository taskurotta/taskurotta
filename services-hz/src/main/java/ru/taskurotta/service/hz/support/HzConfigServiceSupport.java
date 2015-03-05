package ru.taskurotta.service.hz.support;


import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.DistributedObjectEvent;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.service.config.model.ActorPreferences;
import ru.taskurotta.util.ActorUtils;

import javax.annotation.PostConstruct;

/**
 * Designed to populate distributed ActorPreferences map at runtime.
 * If a new task queue object created for absent(unregistered) actor config,
 * that config would be automatically appended to configuration
 *
 * User: dimadin
 * Date: 05.09.13 11:56
 */
public class HzConfigServiceSupport implements DistributedObjectListener {

    private static final Logger logger = LoggerFactory.getLogger(HzConfigServiceSupport.class);
    private String queuePrefix;
    private HazelcastInstance hzInstance;
    private String actorPreferencesMapName;
    private IMap<String, ActorPreferences> distributedActorPreferences;

    @PostConstruct
    private void init() {
        hzInstance.addDistributedObjectListener(this);
        distributedActorPreferences = hzInstance.getMap(actorPreferencesMapName);
    }

    @Override
    public void distributedObjectCreated(DistributedObjectEvent event) {
        DistributedObject obj = event.getDistributedObject();

        if (isActorQueue(obj)) {
            String actorId = ActorUtils.getPrefixStripped(obj.getName(), queuePrefix);
            ActorPreferences ap = new ActorPreferences();
            ap.setId(actorId);
            ap.setBlocked(false);
            ap.setQueueName(obj.getName());

            if (distributedActorPreferences.putIfAbsent(actorId, ap) == null) {
                logger.info("New actor [{}] has been registered", actorId);
            }
        }
    }

    @Override
    public void distributedObjectDestroyed(DistributedObjectEvent event) {
        DistributedObject obj = event.getDistributedObject();

        if (isActorQueue(obj)) {
            String actorId = ActorUtils.getPrefixStripped(obj.getName(), queuePrefix);
            if (distributedActorPreferences.remove(actorId) != null) {
                logger.info("Actor [{}] preferences have been removed", actorId);
            }
        }
    }


    private boolean isActorQueue(DistributedObject obj) {
        return obj instanceof CachedQueue && obj.getName().startsWith(queuePrefix);
    }

    public void setQueuePrefix(String queuePrefix) {
        this.queuePrefix = queuePrefix;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    public void setActorPreferencesMapName(String actorPreferencesMapName) {
        this.actorPreferencesMapName = actorPreferencesMapName;
    }
}

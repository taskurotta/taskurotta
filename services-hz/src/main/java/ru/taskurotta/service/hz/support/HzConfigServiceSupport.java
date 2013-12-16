package ru.taskurotta.service.hz.support;


import javax.annotation.PostConstruct;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.DistributedObjectEvent;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.config.model.ActorPreferences;

/**
 * Designed to populate distributed ActorPreferences map at runtime.
 * If a new task queue object created for absent(unregistered) actor config,
 * that config would be automatically appended to configuration
 * <p/>
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

        if ((obj instanceof IQueue) && (obj.getName().startsWith(queuePrefix))) {//created new task queue object
            String actorId = obj.getName().substring(queuePrefix.length());
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
        //do nothing
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

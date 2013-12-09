package ru.taskurotta.backend.hz.support;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.DistributedObjectEvent;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.config.model.ActorPreferences;

import javax.annotation.PostConstruct;

/**
 * Designed to populate distributed ActorPreferences map at runtime.
 * If a new task queue object created for absent(unregistered) actor config,
 * that config would be automatically appended to configuration
 *
 * User: dimadin
 * Date: 05.09.13 11:56
 */
public class HzConfigBackendSupport implements DistributedObjectListener {

    private static final Logger logger  = LoggerFactory.getLogger(HzConfigBackendSupport.class);
    private String queuePrefix;
    private HazelcastInstance hzInstance;
    private String actorPreferencesMapName;

    @PostConstruct
    private void init() {
        hzInstance.addDistributedObjectListener(this);
    }

    @Override
    public void distributedObjectCreated(DistributedObjectEvent event) {
        DistributedObject obj = event.getDistributedObject();

        if ((obj instanceof IQueue) && (obj.getName().startsWith(queuePrefix))) {//created new task queue object
            ILock lock = hzInstance.getLock(actorPreferencesMapName);
            try {
                lock.lock();
                String actorId = obj.getName().substring(queuePrefix.length());
                if (!isActorConfigExists(actorId)) {//has no distributed config for this actor -> create it
                    ActorPreferences ap = new ActorPreferences();
                    ap.setId(actorId);
                    ap.setBlocked(false);
                    ap.setQueueName(obj.getName());

                    IMap<String, ActorPreferences> distributedActorPreferences = hzInstance.getMap(actorPreferencesMapName);
                    distributedActorPreferences.set(actorId, ap);
                    logger.info("New actor [{}] has been registered", actorId);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private boolean isActorConfigExists(String actorId) {
        IMap<String, ActorPreferences> actorPrefs = hzInstance.getMap(actorPreferencesMapName);
        return actorPrefs.containsKey(actorId);
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

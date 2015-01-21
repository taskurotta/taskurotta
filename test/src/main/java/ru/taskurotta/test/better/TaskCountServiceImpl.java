package ru.taskurotta.test.better;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by greg on 20/01/15.
 */
@Component
public class TaskCountServiceImpl implements TaskCountService {

    private Logger log = LoggerFactory.getLogger(TaskCountServiceImpl.class);


    private ConcurrentHashMap<String, IQueue> queueCache = new ConcurrentHashMap<>();
    private HazelcastInstance hazelcastInstance;


    public TaskCountServiceImpl(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public IQueue getQueueIfInitialized(HazelcastInstance hzInstance, String name) {

        IQueue queue = queueCache.get(name);
        if (queue != null) {
            return queue;
        }

        for (DistributedObject distributedObject : hzInstance.getDistributedObjects()) {
            if ((distributedObject instanceof IQueue) && name.equals(distributedObject.getName())) {
                queue = (IQueue) distributedObject;
                return queueCache.putIfAbsent(name, queue);
            }
        }

        return null;
    }

    @Override
    public int activateTaskCount(String queueName) {
        IQueue queue = getQueueIfInitialized(hazelcastInstance, queueName);
        if (queue != null) {
            return queue.size();
        } else {
            log.debug("Queue {} still not initialized {}", hazelcastInstance, queueName);
            return 0;
        }
    }
}

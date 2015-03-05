package ru.taskurotta.test.better;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;

import java.util.Queue;
import java.util.Set;

/**
 * Created by greg on 20/01/15.
 */
public class TaskCountServiceImpl implements TaskCountService {

    private static final Logger log = LoggerFactory.getLogger(TaskCountServiceImpl.class);

    private HazelcastInstance hazelcastInstance;

    public TaskCountServiceImpl() {
        Set<HazelcastInstance> allHazelcastInstances = Hazelcast.getAllHazelcastInstances();
        hazelcastInstance = allHazelcastInstances.size() == 1 ? Hazelcast.getAllHazelcastInstances().iterator().next() : null;
        if (hazelcastInstance == null) {
            throw new IllegalArgumentException("Hazelcast instance is null");
        }
    }


    @Override
    public int getMaxQueuesSize() {
        int max = 0;
        for (DistributedObject distributedObject : hazelcastInstance.getDistributedObjects()) {
            if (distributedObject instanceof CachedQueue) {
                Queue queue = (CachedQueue) distributedObject;
                int size = queue.size();
                max = Math.max(max, size);
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("MaxQueuesSize = {}", max);
        }

        return max;
    }

}

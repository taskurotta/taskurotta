package ru.taskurotta.hazelcast.util;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;

/**
 * Utility methods set for HZ cluster
 * Date: 30.01.14 13:06
 */
public class ClusterUtils {

    private static final Logger logger = LoggerFactory.getLogger(ClusterUtils.class);

    public static boolean isLocalCachedQueue(HazelcastInstance hazelcastInstance, CachedQueue queue) {
        Partition queuePartition = hazelcastInstance.getPartitionService().getPartition(queue.getPartitionKey());
        Member queueOwner = queuePartition.getOwner();

        boolean result = queueOwner.localMember();

        logger.debug("CachedQueue [{}] is local [{}]", queue.getName(), result);

        return result;
    }

    public static boolean isLocalCachedQueue(HazelcastInstance hazelcastInstance, String queueName) {
        CachedQueue cachedQueue = hazelcastInstance.getDistributedObject(CachedQueue.class
                .getName(), queueName);

        return isLocalCachedQueue(hazelcastInstance, cachedQueue);
    }

}

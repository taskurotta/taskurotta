package ru.taskurotta.hazelcast.util;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods set for HZ cluster
 * Date: 30.01.14 13:06
 */
public class ClusterUtils {

    private static final Logger logger = LoggerFactory.getLogger(ClusterUtils.class);

    public static boolean isLocalQueue(String queueName, HazelcastInstance hazelcastInstance) {
        IQueue queue = hazelcastInstance.getQueue(queueName);
        Partition queuePartition = hazelcastInstance.getPartitionService().getPartition(queue.getPartitionKey());
        Member queueOwner = queuePartition.getOwner();

        boolean result = queueOwner.localMember();

        logger.debug("Queue [{}] is local [{}]", queueName, result);

        return result;
    }

}
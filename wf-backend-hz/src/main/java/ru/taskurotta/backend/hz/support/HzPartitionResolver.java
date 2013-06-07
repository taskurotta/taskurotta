package ru.taskurotta.backend.hz.support;

import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Support class for resolving partition keys for hazelcast
 * User: dudin
 * Date: 07.06.13 12:45
 */
public class HzPartitionResolver implements MembershipListener {

    private static final Logger logger = LoggerFactory.getLogger(HzPartitionResolver.class);

    private int nodesCount = 1;

    public Object resolveByUUID(UUID uuid) {
        int result = 1;
        if(uuid!=null && nodesCount>0) {
            result = Math.abs(uuid.hashCode()%nodesCount) + 1;
        }
        logger.debug("Partition key getted for uuid[{}] is [{}]", uuid, result);
        return result;
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        this.nodesCount = membershipEvent.getCluster().getMembers().size();
        logger.debug("Cluster member added, count is[{}]", this.nodesCount);
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        this.nodesCount = membershipEvent.getCluster().getMembers().size();
        logger.debug("Cluster member removed, count is[{}]", this.nodesCount);
    }
}

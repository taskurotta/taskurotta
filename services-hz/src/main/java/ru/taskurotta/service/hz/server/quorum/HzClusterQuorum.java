package ru.taskurotta.service.hz.server.quorum;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.server.quorum.ClusterQuorum;

import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class HzClusterQuorum implements MembershipListener, ClusterQuorum {

    private static final Logger logger = LoggerFactory.getLogger(HzClusterQuorum.class);

    private final AtomicInteger membership = new AtomicInteger(1);
    private Cluster cluster;
    private byte quorumSize;

    public final void ClusterMembershipListener(Cluster cluster, byte quorumSize) {
        this.cluster = cluster;
        this.quorumSize = quorumSize;

        setMembershipRef();
        cluster.addMembershipListener(this);
    }

    private void setMembershipRef() {
        membership.set(cluster.getMembers().size());

        logger.error("Membership changed to " + membership.get());
    }

    public boolean isPresent() {
        return membership.get() >= quorumSize;
    }

    @Override
    public final void memberAdded(MembershipEvent membershipEvent) {
        setMembershipRef();
    }

    @Override
    public final void memberRemoved(MembershipEvent membershipEvent) {
        setMembershipRef();
    }

    @Override
    public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
    }
}
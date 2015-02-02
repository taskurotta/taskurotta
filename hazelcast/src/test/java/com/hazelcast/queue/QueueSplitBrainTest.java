package com.hazelcast.queue;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.instance.GroupProperties;
import com.hazelcast.instance.HazelcastInstanceFactory;
import com.hazelcast.instance.Node;
import com.hazelcast.instance.TestUtil;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.taskurotta.hazelcast.queue.CachedQueue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class QueueSplitBrainTest extends HazelcastTestSupport {

    @Before
    @After
    public void killAllHazelcastInstances() throws IOException {
        HazelcastInstanceFactory.shutdownAll();
    }

    @Test
    @Ignore
    public void testQueueSplitBrain() throws InterruptedException {
        Config config = newConfig();
        HazelcastInstance h1 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance h2 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance h3 = Hazelcast.newHazelcastInstance(config);
        final String name = generateKeyOwnedBy(h1);
        CachedQueue<Object> queue = h1.getDistributedObject(CachedQueue.class.getName(), name);

        TestMemberShipListener memberShipListener = new TestMemberShipListener(2);
        h3.getCluster().addMembershipListener(memberShipListener);
        TestLifeCycleListener lifeCycleListener = new TestLifeCycleListener(1);
        h3.getLifecycleService().addLifecycleListener(lifeCycleListener);

        for (int i = 0; i < 100; i++) {
            queue.add("item" + i);
        }

        waitAllForSafeState();

        closeConnectionBetween(h1, h3);
        closeConnectionBetween(h2, h3);

        assertOpenEventually(memberShipListener.latch);
        assertClusterSizeEventually(2,h1);
        assertClusterSizeEventually(2, h2);
        assertClusterSizeEventually(1, h3);

        for (int i = 100; i < 200; i++) {
            queue.add("item" + i);
        }

        CachedQueue<Object> queue3 = h3.getDistributedObject(CachedQueue.class.getName(), name);
        for (int i = 0; i < 50; i++) {
            queue3.add("lostQueueItem" + i);
        }

        assertOpenEventually(lifeCycleListener.latch);
        assertClusterSizeEventually(3, h1);
        assertClusterSizeEventually(3, h2);
        assertClusterSizeEventually(3, h3);

        CachedQueue<Object> testQueue = h1.getDistributedObject(CachedQueue.class.getName(), name);
        assertFalse(testQueue.contains("lostQueueItem0"));
        assertFalse(testQueue.contains("lostQueueItem49"));
        assertTrue(testQueue.contains("item0"));
        assertTrue(testQueue.contains("item199"));
        assertTrue(testQueue.contains("item121"));
        assertTrue(testQueue.contains("item45"));
    }

    private void closeConnectionBetween(HazelcastInstance h1, HazelcastInstance h2) {
        if (h1 == null || h2 == null) return;
        final Node n1 = TestUtil.getNode(h1);
        final Node n2 = TestUtil.getNode(h2);
        n1.clusterService.removeAddress(n2.address);
        n2.clusterService.removeAddress(n1.address);
    }

    private Config newConfig() {
        Config config = new Config();
        config.setProperty(GroupProperties.PROP_MERGE_FIRST_RUN_DELAY_SECONDS, "30");
        config.setProperty(GroupProperties.PROP_MERGE_NEXT_RUN_DELAY_SECONDS, "3");
        return config;
    }

    private class TestLifeCycleListener implements LifecycleListener {

        CountDownLatch latch;

        TestLifeCycleListener(int countdown) {
            this.latch = new CountDownLatch(countdown);
        }

        @Override
        public void stateChanged(LifecycleEvent event) {
            if (event.getState() == LifecycleEvent.LifecycleState.MERGED) {
                latch.countDown();
            }
        }
    }

    private class TestMemberShipListener implements MembershipListener {

        final CountDownLatch latch;

        TestMemberShipListener(int countdown) {
            this.latch = new CountDownLatch(countdown);
        }

        @Override
        public void memberAdded(MembershipEvent membershipEvent) {

        }

        @Override
        public void memberRemoved(MembershipEvent membershipEvent) {
            latch.countDown();
        }

        @Override
        public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {

        }
    }
}

/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.queue;

import com.hazelcast.config.Config;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.taskurotta.hazelcast.queue.CachedQueue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(HazelcastParallelClassRunner.class)
@Category(QuickTest.class)
public class ClusterQueueTest extends HazelcastTestSupport {


    /**
     * Test for issue 730. (google)
     */
    @Test
    public void testDeadTaker() throws Exception {
        Config config = new Config();
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        config.addListenerConfig(new ListenerConfig().setImplementation(new MembershipListener() {
            public void memberAdded(MembershipEvent membershipEvent) {
            }

            public void memberRemoved(MembershipEvent membershipEvent) {
                shutdownLatch.countDown();
            }

            public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
            }
        }));

        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        final HazelcastInstance[] instances = factory.newInstances(config);
        final HazelcastInstance h1 = instances[0];
        final HazelcastInstance h2 = instances[1];
        warmUpPartitions(h1, h2);

        final CachedQueue q1 = h1.getDistributedObject(CachedQueue.class.getName(), "default");
        final CachedQueue q2 = h2.getDistributedObject(CachedQueue.class.getName(), "default");

        final CountDownLatch startLatch = new CountDownLatch(1);
        new Thread(new Runnable() {
            public void run() {
                try {
                    assertTrue(startLatch.await(10, TimeUnit.SECONDS)); // fail shutdown if await fails.
                    Thread.sleep(5000);
                    h2.getLifecycleService().terminate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                try {
                    startLatch.countDown();
                    final Object o = q2.take();
                    fail("Should not be able to take: " + o);
                } catch (HazelcastInstanceNotActiveException ignored) {
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        assertTrue(shutdownLatch.await(1, TimeUnit.MINUTES));

        q1.offer("item");
        assertEquals(1, q1.size());   // 0
        assertEquals("item", q1.poll());
    }


    @Test
    public void testPollNull() throws Exception {
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        final HazelcastInstance[] instances = factory.newInstances();
        final HazelcastInstance h1 = instances[0];
        final HazelcastInstance h2 = instances[1];
        final CachedQueue q1 = h1.getDistributedObject(CachedQueue.class.getName(), "default");
        final CachedQueue q2 = h2.getDistributedObject(CachedQueue.class.getName(), "default");
        for (int i = 0; i < 100; i++) {
            assertNull(q1.poll());
            assertNull(q2.poll());
        }
        assertNull(q1.poll(2, TimeUnit.SECONDS));
        assertNull(q2.poll(2, TimeUnit.SECONDS));
    }

    @Test
    public void testTake() throws Exception {
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        final HazelcastInstance[] instances = factory.newInstances();
        final HazelcastInstance h1 = instances[0];
        final HazelcastInstance h2 = instances[1];
        final CachedQueue q1 = h1.getDistributedObject(CachedQueue.class.getName(), "default");
        final CachedQueue q2 = h2.getDistributedObject(CachedQueue.class.getName(), "default");
        final CountDownLatch offerLatch = new CountDownLatch(2 * 100);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(3000);
                    for (int i = 0; i < 100; i++) {
                        if (q1.offer("item")) {
                            offerLatch.countDown();
                        }
                        if (q2.offer("item")) {
                            offerLatch.countDown();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        assertOpenEventually(offerLatch);

        final ExecutorService es = Executors.newFixedThreadPool(50);
        final CountDownLatch latch = new CountDownLatch(200);
        for (int i = 0; i < 100; i++) {
            es.execute(new Runnable() {
                public void run() {
                    try {
                        if ("item".equals(q1.take())) {
                            latch.countDown();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            es.execute(new Runnable() {
                public void run() {
                    try {
                        if ("item".equals(q2.take())) {
                            latch.countDown();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        assertOpenEventually(latch);
        es.shutdown();
    }

    @Test
    public void testPollLong() throws Exception {
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        final HazelcastInstance[] instances = factory.newInstances();
        final HazelcastInstance h1 = instances[0];
        final HazelcastInstance h2 = instances[1];
        final CachedQueue q1 = h1.getDistributedObject(CachedQueue.class.getName(), "default");
        final CachedQueue q2 = h2.getDistributedObject(CachedQueue.class.getName(), "default");
        final CountDownLatch offerLatch = new CountDownLatch(2 * 100);
        Thread.sleep(1000);
        new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 100; i++) {
                    if (q1.offer("item")) {
                        offerLatch.countDown();
                    }
                    if (q2.offer("item")) {
                        offerLatch.countDown();
                    }
                }
            }
        }).start();
        assertOpenEventually(offerLatch);
        final ExecutorService es = Executors.newFixedThreadPool(50);
        final CountDownLatch latch = new CountDownLatch(200);
        Thread.sleep(3000);
        for (int i = 0; i < 100; i++) {
            es.execute(new Runnable() {
                public void run() {
                    try {
                        if ("item".equals(q1.poll(5, TimeUnit.SECONDS))) {
                            latch.countDown();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            es.execute(new Runnable() {
                public void run() {
                    try {
                        if ("item".equals(q2.poll(5, TimeUnit.SECONDS))) {
                            latch.countDown();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        assertOpenEventually(latch);
        es.shutdown();
    }

    /**
     * Test case for issue 289.
     * <p/>
     * 1. Create instanceA then instanceB, and then a queue on each (same queue name)
     * 2. put a message on queue from instanceB
     * 3. take message off on instanceA
     * 4. shutdown instanceA, then check if queue is still empty on instanceB
     *
     * @throws Exception
     */
    @Test
    public void testQueueAfterShutdown() throws Exception {
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        final HazelcastInstance[] instances = factory.newInstances();
        final HazelcastInstance h1 = instances[0];
        final HazelcastInstance h2 = instances[1];
        CachedQueue q1 = h1.getDistributedObject(CachedQueue.class.getName(), "default");
        CachedQueue q2 = h2.getDistributedObject(CachedQueue.class.getName(), "default");
        q2.offer("item");
        assertEquals(1, q1.size());
        assertEquals(1, q2.size());
        assertEquals("item", q1.take());
        assertEquals(0, q1.size());
        assertEquals(0, q2.size());
        h1.getLifecycleService().shutdown();
        assertEquals(0, q2.size());
    }

    /**
     * @throws Exception
     */
    @Test
    public void testQueueAfterShutdown2() throws Exception {
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        final HazelcastInstance[] instances = factory.newInstances();
        final HazelcastInstance h1 = instances[0];
        final HazelcastInstance h2 = instances[1];
        CachedQueue q1 = h1.getDistributedObject(CachedQueue.class.getName(), "default");
        CachedQueue q2 = h2.getDistributedObject(CachedQueue.class.getName(), "default");
        q1.offer("item");
        assertEquals(1, q1.size());
        assertEquals(1, q2.size());
        assertEquals("item", q2.take());
        assertEquals(0, q1.size());
        assertEquals(0, q2.size());
        h2.getLifecycleService().shutdown();
        assertEquals(0, q1.size());
    }



    private HazelcastInstance[] createHazelcastInstances() {
        Config config = new Config();
        final String configName = randomString();
        config.getQueueConfig(configName).setMaxSize(100);
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(2);
        return factory.newInstances(config);
    }

}

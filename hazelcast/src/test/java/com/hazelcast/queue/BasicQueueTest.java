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

import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.taskurotta.hazelcast.queue.CachedQueue;

import static org.junit.Assert.*;


@RunWith(HazelcastParallelClassRunner.class)
@Category(QuickTest.class)
public class BasicQueueTest extends AbstractQueueTest {


    // ================ offer ==============================

    @Test
    public void testOffer() throws Exception {
        int count = 100;
        CachedQueue<String> queue = newCachedQueue();
        for (int i = 0; i < count; i++) {
            queue.offer("item" + i);
        }

        assertEquals(100, queue.size());
    }


    @Test
    public void testOffer_whenNullArgument() {
        CachedQueue<String> queue = newCachedQueue();
        try {
            queue.offer(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(queue.isEmpty());
    }


    // ================ poll ==============================

    @Test
    public void testPoll() {
        int count = 100;
        CachedQueue<String> queue = newCachedQueue();
        for (int i = 0; i < count; i++) {
            queue.offer("item" + i);
        }

        queue.poll();
        queue.poll();
        queue.poll();
        queue.poll();
        assertEquals(96, queue.size());
    }

    @Test
    public void testPoll_whenQueueEmpty() {
        CachedQueue<String> queue = newCachedQueue();
        assertNull(queue.poll());
    }



    @Test
    public void testRemoveAll_whenCollectionNull() {
        CachedQueue<String> queue = newCachedQueue();
        queue.add("item3");
        queue.add("item4");
        queue.add("item5");

        try {
            queue.removeAll(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(3, queue.size());
    }

}

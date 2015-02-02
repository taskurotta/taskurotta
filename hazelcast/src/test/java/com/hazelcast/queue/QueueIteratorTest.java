package com.hazelcast.queue;

import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.taskurotta.hazelcast.queue.CachedQueue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

@RunWith(HazelcastParallelClassRunner.class)
@Category(QuickTest.class)
public class QueueIteratorTest extends AbstractQueueTest {

    @Test
    @Ignore
    public void testIterator() {
        CachedQueue<String> queue = newCachedQueue();
        for (int i = 0; i < 10; i++) {
            queue.offer("item" + i);
        }
        Iterator<String> iterator = queue.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Object o = iterator.next();
            assertEquals(o, "item" + i++);
        }
    }

    @Test
    @Ignore
    public void testIterator_whenQueueEmpty() {
        CachedQueue<String> queue = newCachedQueue();
        Iterator<String> iterator = queue.iterator();

        assertFalse(iterator.hasNext());
        try {
            assertNull(iterator.next());
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    @Ignore
    public void testIteratorRemove() {
        CachedQueue<String> queue = newCachedQueue();
        for (int i = 0; i < 10; i++) {
            queue.offer("item" + i);
        }

        Iterator<String> iterator = queue.iterator();
        iterator.next();
        try {
            iterator.remove();
            fail();
        } catch (UnsupportedOperationException e) {
        }

        assertEquals(10, queue.size());
    }
}

package ru.taskurotta.hz.test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class MemoryLeakTest {

    @Ignore
    @Test
    public void test() {


        HazelcastInstance hz = Hazelcast.newHazelcastInstance();
        final IMap<Object, Object> testMap = hz.getMap("test");


        ExecutorService executorService = Executors.newFixedThreadPool(1);

        final AtomicInteger keySeq = new AtomicInteger(0);
        final AtomicBoolean done = new AtomicBoolean(false);

        while (true) {
            int uniquePart = keySeq.incrementAndGet();
            done.set(false);

            final int[] fatKey = new int[1000];
            fatKey[0] = uniquePart;

            testMap.lock(fatKey);


            executorService.execute(new Runnable() {
                @Override
                public void run() {

                    testMap.lock(fatKey);
                    testMap.unlock(fatKey);

                    done.set(true);
                }
            });

            // waiting second thread lock
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException e) {
            }

            testMap.unlock(fatKey);

            while (!done.get()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
            }

        }
    }

}

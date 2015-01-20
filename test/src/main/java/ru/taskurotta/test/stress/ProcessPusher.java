package ru.taskurotta.test.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * todo: add correct shutdown hooks to stop threads
 */
public class ProcessPusher {

    // per second

    public ProcessPusher(final HazelcastInstance hazelcastInstance, final long speed, final int threadCount, final int
            queueSizeThreshold) {

        final Queue queue = new ConcurrentLinkedQueue();

        // start planner thread
        new Thread() {

            @Override
            public void run() {

                // queue should be fulled up to 10 seconds to future
                long maxSizeLimit = speed * 10l;

                long timeCursor = System.currentTimeMillis();

                while (true) {

                    int maxQueuesSize = getMaxQueuesSize(hazelcastInstance);

                    if (maxQueuesSize > queueSizeThreshold) {
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                        }

                        // reset cursor;

                        timeCursor = System.currentTimeMillis();

                        continue;
                    }


                    int currSize = queue.size();

                    if (currSize < maxSizeLimit) {

                        double interval = 1000l / speed;

                        for (int i = 0; i < maxSizeLimit - currSize; i++) {
                            queue.add(timeCursor += interval);
                        }

                        continue;
                    }


                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                    }

                }
            }

            private int getMaxQueuesSize(HazelcastInstance hazelcastInstance) {

                int max = 0;

                for (DistributedObject distributedObject : hazelcastInstance.getDistributedObjects()) {
                    if (distributedObject instanceof IQueue) {
                        Queue queue = (IQueue) distributedObject;
                        int size = queue.size();
                        max = Math.max(max, size);

                    }
                }

                return max;
            }

        }.start();


        // start pusher threads
        for (int i = 0; i < threadCount; i++) {

            new Thread() {
                @Override
                public void run() {


                    while (true) {

                        Long timeToStart = (Long) queue.poll();

                        if (timeToStart == null) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            } catch (InterruptedException e) {
                            }

                            continue;
                        }

                        long currTime = System.currentTimeMillis();

                        if (currTime < timeToStart) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(timeToStart - currTime);
                            } catch (InterruptedException e) {
                            }
                        }

                        StressTaskCreator.sendOneTask();
                    }
                }
            }.start();
        }


    }

}

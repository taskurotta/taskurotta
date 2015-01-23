package ru.taskurotta.test.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import ru.taskurotta.server.GeneralTaskServer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ProcessPusher {

    // per second

    public ProcessPusher(final HazelcastInstance hazelcastInstance, final int maxProcessQuantity, final long
            startProcessPresSecond, final int threadCount, final int queueSizesThreshold, final int
            runningProcessThreshold) {

        final Queue queue = new ConcurrentLinkedQueue();

        // queue should be fulled up to 1 seconds to future
        final long maxSizeLimit = startProcessPresSecond * 1l;

        // start planner thread
        new DaemonThread("process planner", TimeUnit.SECONDS, 1) {

            AtomicInteger counter = new AtomicInteger(0);

            @Override
            public void daemonJob() {

                int sumQueuesSize = getSumQueuesSize(hazelcastInstance);

                // should waiting to prevent overload
                if (sumQueuesSize > queueSizesThreshold || counter.get() - GeneralTaskServer
                        .finishedProcessesCounter.get() > runningProcessThreshold) {

                    return;
                }


                int currSize = queue.size();

                if (currSize < maxSizeLimit) {

                    double interval = 1000l / startProcessPresSecond;
                    double timeCursor = System.currentTimeMillis();

                    for (int i = 0; i < maxSizeLimit - currSize; i++) {
                        timeCursor += interval;

                        queue.add((long) (timeCursor));

                        if (counter.incrementAndGet() == maxProcessQuantity) {
                            throw new DaemonThread.StopSignal();
                        }
                    }

                    return;
                }

            }

            private int getSumQueuesSize(HazelcastInstance hazelcastInstance) {

                int sum = 0;

                for (DistributedObject distributedObject : hazelcastInstance.getDistributedObjects()) {
                    if (distributedObject instanceof IQueue) {
                        Queue queue = (IQueue) distributedObject;
                        sum += queue.size();
                    }
                }

                return sum;
            }

        }.start();


        // start pusher threads
        for (int i = 0; i < threadCount; i++) {

            new DaemonThread("process pusher " + i, null, 0) {

                @Override
                public void daemonJob() {

                    Long timeToStart = (Long) queue.poll();

                    if (timeToStart == null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                        }

                        return;
                    }

                    long currTime = System.currentTimeMillis();

                    if (currTime < timeToStart.longValue()) {

                        try {
                            TimeUnit.MILLISECONDS.sleep(timeToStart.longValue() - currTime);
                        } catch (InterruptedException e) {
                        }
                    }

                    StressTaskCreator.sendOneTask();
                }
            }.start();

        }
    }


}

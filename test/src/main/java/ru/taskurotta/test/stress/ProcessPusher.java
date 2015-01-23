package ru.taskurotta.test.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.test.stress.process.Starter;
import ru.taskurotta.test.stress.util.DaemonThread;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ProcessPusher {

    private final static Logger logger = LoggerFactory.getLogger(LifetimeProfiler.class);

    public static AtomicInteger counter = new AtomicInteger(0);

    // per second

    public ProcessPusher(final Starter starter, final HazelcastInstance hazelcastInstance, final int maxProcessQuantity,
                         final int initialProcessPresSecondPush, final int threadCount, final int minQueuesSize,
                         final int maxQueuesSize) {

        final Queue queue = new ConcurrentLinkedQueue();

        // start planner thread
        new DaemonThread("process planner", TimeUnit.SECONDS, 1) {

            int currentSpeedPerSecond = initialProcessPresSecondPush;

            @Override
            public void daemonJob() {


                int sumQueuesSize = getSumQueuesSize(hazelcastInstance);

                // should waiting to prevent overload
                if (sumQueuesSize > maxQueuesSize) {

                    // go slowly
                    currentSpeedPerSecond --;
                    return;
                }


                if (sumQueuesSize < minQueuesSize) {

                    // go faster
                    currentSpeedPerSecond++;
                }


                int currSize = queue.size();

                if (currSize < currentSpeedPerSecond) {

                    int actualSpeed = currentSpeedPerSecond;

                    int needToPush = actualSpeed - currSize;

                    logger.info("Speed pps = " + actualSpeed);

                    double interval = 1000l / actualSpeed;
                    double timeCursor = System.currentTimeMillis();

                    for (int i = 0; i < needToPush; i++) {
                        timeCursor += interval;

                        queue.add((long) (timeCursor));

                        if (counter.incrementAndGet() == maxProcessQuantity) {
                            throw new StopSignal();
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

                    starter.start();
                }
            }.start();

        }
    }


}

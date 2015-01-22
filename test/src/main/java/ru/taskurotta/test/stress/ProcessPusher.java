package ru.taskurotta.test.stress;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import ru.taskurotta.server.GeneralTaskServer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import static ru.taskurotta.test.stress.LifetimeProfiler.startedProcessCounter;


public class ProcessPusher {

    // per second

    public ProcessPusher(final HazelcastInstance hazelcastInstance, final long speed, final int threadCount, final int
            queueSizeThreshold, final int runningProcessThreshold) {

        final Queue queue = new ConcurrentLinkedQueue();

        // queue should be fulled up to 1 seconds to future
        final long maxSizeLimit = speed * 1l;

        // start planner thread
        new DaemonThread("process planner", TimeUnit.SECONDS, 1) {

            @Override
            public void daemonJob() {



                int maxQueuesSize = getMaxQueuesSize(hazelcastInstance);

                if (maxQueuesSize > queueSizeThreshold ||  startedProcessCounter.get() - GeneralTaskServer
                        .finishedProcessesCounter.get() > runningProcessThreshold) {

//                    if (startedProcessCounter.get() - GeneralTaskServer
//                            .finishedProcessesCounter.get() > runningProcessThreshold) {
//                        throw new DaemonThread.StopSignal();
//                    }

                    return;
                }


                int currSize = queue.size();

                if (currSize < maxSizeLimit) {

                    double interval = 1000l / speed;
                    double timeCursor = System.currentTimeMillis();

                    for (int i = 0; i < maxSizeLimit - currSize; i++) {
                        timeCursor += interval;

                        queue.add((long) (timeCursor));
                    }

                    return;
                }

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

    private static int getMaxQueuesSize(HazelcastInstance hazelcastInstance) {

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

}

package ru.taskurotta.test.better;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by greg on 20/01/15.
 */
public class SimpleTestRunner {

    private Logger log = LoggerFactory.getLogger(SimpleTestRunner.class);
    private FullFeatureDeciderClient decider;
    private int bundleSize;
    private long duration;
    private int threshold;
    private int threadSize;
    private long speed;
    private ExecutorService executorService;
    private TaskCountService taskCountService;
    private ClientServiceManager clientServiceManager;
    private final Queue<Long> queue = new ConcurrentLinkedQueue<>();

    private long startTime = System.currentTimeMillis();

    private CountDownLatch latch;


    public void initAndStart() throws InterruptedException {
        Set<HazelcastInstance> allHazelcastInstances = Hazelcast.getAllHazelcastInstances();
        HazelcastInstance hazelcastInstance = allHazelcastInstances.size() == 1 ? Hazelcast.getAllHazelcastInstances().iterator().next() : null;
        taskCountService = new TaskCountServiceImpl(hazelcastInstance);
        executorService = Executors.newFixedThreadPool(threadSize);
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        decider = deciderClientProvider.getDeciderClient(FullFeatureDeciderClient.class);
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000); //wait actors
                    start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void start() throws InterruptedException {
        log.info("Starting new test...");
        log.info("Bundle size = {}", bundleSize);
        log.info("Threshold = {}", threshold);
        latch = new CountDownLatch(1);
        firstStart();
        for (int t = 0; t < threadSize; t++) {
            final Runnable task = new Runnable() {
                @Override
                public void run() {
                    while (isConditionToRunTrue()) {
                        checkProcessInQueue();
                    }
                    log.info("Test finished");
                    latch.countDown();
                }
            };
            executorService.execute(task);
        }
        latch.await();
        System.exit(0);
    }

    private void firstStart() {
        int currSize = queue.size();
        final long maxSizeLimit = speed * 1l;
        if (currSize < maxSizeLimit) {
            double interval = 1000l / speed;
            double timeCursor = System.currentTimeMillis();

            for (int i = 0; i < maxSizeLimit - currSize; i++) {
                timeCursor += interval;

                queue.add((long) (timeCursor));
            }
        }
    }

    private void checkProcessInQueue() {
        Long timeToStart = queue.poll();

        if (timeToStart == null) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

        long currTime = System.currentTimeMillis();

        if (currTime < timeToStart) {

            try {
                TimeUnit.MILLISECONDS.sleep(timeToStart - currTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Process started");
        decider.start();
    }

    private boolean isConditionToRunTrue() {
        return startTime + duration >= System.currentTimeMillis();
    }

    private boolean isConditionToAddMoreTask() {
        int maxTasks = taskCountService.getMaxQueuesSize();
        return maxTasks < threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setDecider(FullFeatureDeciderClient decider) {
        this.decider = decider;
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void setBundleSize(int bundleSize) {
        this.bundleSize = bundleSize;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public void setThreadSize(int threadSize) {
        this.threadSize = threadSize;
    }


    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public void setTaskCountService(TaskCountService taskCountService) {
        this.taskCountService = taskCountService;
    }


}

package ru.taskurotta.test.better;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by greg on 20/01/15.
 */
public class SimpleTestRunner {

    private Logger log = LoggerFactory.getLogger(SimpleTestRunner.class);
    private FullFeatureDeciderClient decider;
    private long duration;
    private int threadSize;
    private int queueSizeThreshold;
    private long speed;
    private ExecutorService executorService;
    private TaskCountService taskCountService;
    private ClientServiceManager clientServiceManager;
    private final Queue<Long> queue = new ConcurrentLinkedQueue<>();
    private long startTime = System.currentTimeMillis();
    private AtomicBoolean isShutdown = new AtomicBoolean(false);

    private CountDownLatch latch;


    public void initAndStart() throws InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                isShutdown.set(true);
            }
        });
        taskCountService = new TaskCountServiceImpl();
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
        log.info("Thread size = {}", threadSize);
        log.info("Queue size threshold = {}", queueSizeThreshold);
        log.info("Speed = {}", speed);
        log.info("Duration = {}", duration);

        latch = new CountDownLatch(1);
        startProduce();
        startConsume();
        latch.await();
        System.exit(0);
    }

    private void startConsume() {
        for (int t = 0; t < threadSize; t++) {
            final Runnable task = new Runnable() {
                @Override
                public void run() {
                    Thread.currentThread().setName("Consumer thread");
                    while (isConditionToRunTrue()) {
                        checkProcessInQueue();
                    }
                    log.info("Consume finished");
                    latch.countDown();
                }
            };
            executorService.execute(task);
        }
    }

    private void startProduce() {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("Producer thread");
                while (isConditionToRunTrue()) {
                    int maxQueuesSize = taskCountService.getMaxQueuesSize();
                    if (maxQueuesSize > queueSizeThreshold) {
                        continue;
                    }
                    int currSize = queue.size();
                    final long maxSizeLimit = speed;
                    if (currSize < maxSizeLimit) {
                        double interval = 1000l / speed;
                        double timeCursor = System.currentTimeMillis();
                        log.debug("Smooth adding processes {}", maxSizeLimit - currSize);
                        for (int i = 0; i < maxSizeLimit - currSize; i++) {
                            timeCursor += interval;
                            queue.add((long) (timeCursor));
                        }
                    }
                }
            }
        });
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
        decider.start();
    }

    private boolean isConditionToRunTrue() {
        if (isShutdown.get()) return false;
        return startTime + duration >= System.currentTimeMillis();
    }


    public void setDecider(FullFeatureDeciderClient decider) {
        this.decider = decider;
    }

    public void setClientServiceManager(ClientServiceManager clientServiceManager) {
        this.clientServiceManager = clientServiceManager;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }


    public void setThreadSize(int threadSize) {
        this.threadSize = threadSize;
    }


    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public void setQueueSizeThreshold(int queueSizeThreshold) {
        this.queueSizeThreshold = queueSizeThreshold;
    }
}

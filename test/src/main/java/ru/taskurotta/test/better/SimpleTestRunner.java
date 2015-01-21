package ru.taskurotta.test.better;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by greg on 20/01/15.
 */
public class SimpleTestRunner {

    private Logger log = LoggerFactory.getLogger(SimpleTestRunner.class);
    private FullFeatureDeciderClient decider;
    private int threads;

    private int initialSize;
    private int bundleSize;
    private long duration;
    private int threshold;
    private ExecutorService executorService;
    private TaskCountService taskCountService;
    private ClientServiceManager clientServiceManager;

    private final AtomicInteger counter = new AtomicInteger(0);


    private long startTime = System.currentTimeMillis();

    private CountDownLatch countDownLatch;


    public void initAndStart() throws InterruptedException {
        log.info("I'm initializing!");
        log.info("Initial size = {}", initialSize);
        log.info("Bundle size = {}", bundleSize);
        log.info("Threshold = {}", threshold);
        log.info("Threads count = {}", threads);
//        Set<HazelcastInstance> allHazelcastInstances = Hazelcast.getAllHazelcastInstances();
//        HazelcastInstance hazelcastInstance = allHazelcastInstances.size() == 1 ? Hazelcast.getAllHazelcastInstances().iterator().next() : null;

//        taskCountService = new TaskCountServiceImpl(hazelcastInstance);
        executorService = Executors.newFixedThreadPool(threads);
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        decider = deciderClientProvider.getDeciderClient(FullFeatureDeciderClient.class);
        start();
    }

    public void start() throws InterruptedException {
        log.info("Starting new test...");
        countDownLatch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    while (isConditionToRunTrue()) {
                        int count = counter.getAndIncrement();
                        if (bundleSize > count || isConditionToAddMoreTask()) {
                            if (count % threshold == 0) {
                                log.info("Tasks already started {}", count);
                            }
                            decider.start();
                        }
                    }
                    log.info("Thread finished");
                    countDownLatch.countDown();
                }
            };
            executorService.execute(task);
        }
        countDownLatch.await();
        System.exit(0);
    }

    private boolean isConditionToRunTrue() {
        return startTime + duration >= System.currentTimeMillis();
    }

    private boolean isConditionToAddMoreTask() {
//        String deciderQueue = "task_ru.taskurotta.test.fullfeature.decider.FullFeatureDecider#1.0";
//        String workerQueue = "task_ru.taskurotta.test.fullfeature.worker.FullFeatureWorker#1.0";
//        int deciderTaskCount = taskCountService.activateTaskCount(deciderQueue);
//        int workerTaskCount = taskCountService.activateTaskCount(workerQueue);
//        int maxTasks = Math.max(deciderTaskCount, workerTaskCount);
        return false;//maxTasks < threshold;
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

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    public void setBundleSize(int bundleSize) {
        this.bundleSize = bundleSize;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setTaskCountService(TaskCountService taskCountService) {
        this.taskCountService = taskCountService;
    }
}

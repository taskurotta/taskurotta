package ru.taskurotta.test.better;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by greg on 20/01/15.
 */
public class SimpleTestRunner {

    private Logger log = LoggerFactory.getLogger(SimpleTestRunner.class);
    private FullFeatureDeciderClient decider;

    private int bundleSize;
    private long duration;
    private int threshold;
    private ExecutorService executorService;
    private TaskCountService taskCountService;
    private ClientServiceManager clientServiceManager;

    private long startTime = System.currentTimeMillis();

    private CountDownLatch countDownLatch;


    public void initAndStart() throws InterruptedException {
        Set<HazelcastInstance> allHazelcastInstances = Hazelcast.getAllHazelcastInstances();
        HazelcastInstance hazelcastInstance = allHazelcastInstances.size() == 1 ? Hazelcast.getAllHazelcastInstances().iterator().next() : null;

        taskCountService = new TaskCountServiceImpl(hazelcastInstance);
        executorService = Executors.newFixedThreadPool(1);
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        decider = deciderClientProvider.getDeciderClient(FullFeatureDeciderClient.class);
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
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
        countDownLatch = new CountDownLatch(1);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                while (isConditionToRunTrue()) {
                    if (isConditionToAddMoreTask()) {
                        for (int i = 0; i < bundleSize; i++) {
                            if (i % threshold == 0) {
                                log.info("Process to start {}", i);
                            }
                            decider.start();
                        }
                    }
                }
                log.info("Test finished");
                countDownLatch.countDown();
            }
        };
        executorService.execute(task);
        countDownLatch.await();
        System.exit(0);
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

    public void setTaskCountService(TaskCountService taskCountService) {
        this.taskCountService = taskCountService;
    }


}

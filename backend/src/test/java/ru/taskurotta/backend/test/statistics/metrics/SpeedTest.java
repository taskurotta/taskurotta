package ru.taskurotta.backend.test.statistics.metrics;

import org.junit.Ignore;
import org.junit.Test;
import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.metrics.ArrayCheckPoint;
import ru.taskurotta.backend.statistics.metrics.CheckPoint;
import ru.taskurotta.backend.statistics.metrics.MeanCheckPoint;
import ru.taskurotta.backend.statistics.metrics.YammerCheckPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * User: stukushin
 * Date: 05.09.13
 * Time: 16:51
 */
public class SpeedTest {

    private String name = "testName";
    private String actorId = "testActorId";
    private DataListener dataListener = new MockDataListener();
    private Random random = new Random();

    private int size = 100000;
    private long[] data;

    private ExecutorService executorService = Executors.newFixedThreadPool(8);

    class MockDataListener implements DataListener {
        @Override
        public void handle(String name, String actorId, long count, double value, long time) {
            // do nothing
        }
    }

    class MarkTask implements Callable<Long> {

        private CheckPoint checkPoint;

        MarkTask(CheckPoint checkPoint) {
            this.checkPoint = checkPoint;
        }

        @Override
        public Long call() throws Exception {

            long start = System.nanoTime();

            checkPoint.mark(random.nextInt(size));

            return System.nanoTime() - start;
        }
    }

    @Ignore
    @Test
    public void testCheckPoint() throws ExecutionException, InterruptedException {
        List<CheckPoint> checkPoints = new ArrayList<>();
        checkPoints.add(new ArrayCheckPoint(name, actorId, dataListener));
        checkPoints.add(new YammerCheckPoint(name, actorId, dataListener));
        checkPoints.add(new MeanCheckPoint(name, actorId, dataListener));

        Collections.shuffle(checkPoints);

        for (CheckPoint checkPoint : checkPoints) {
            test(checkPoint);
        }
    }

    private void test(CheckPoint checkPoint) throws InterruptedException, ExecutionException {

        List<MarkTask> tasks = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            tasks.add(new MarkTask(checkPoint));
        }

        List<Future<Long>> futures = executorService.invokeAll(tasks);

        long sum = 0;
        Iterator<Future<Long>> iterator = futures.iterator();
        while (!futures.isEmpty()) {
            Future<Long> future = iterator.next();

            if (future.isDone()) {
                long result = future.get();

                sum += result;

                iterator.remove();
            }
        }

        System.out.println("For [" + checkPoint.getClass().getSimpleName() + "] mean = " + sum / size);
    }
}

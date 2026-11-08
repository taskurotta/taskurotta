package ru.taskurotta.service.metrics;

import org.junit.Test;
import ru.taskurotta.service.metrics.handler.DataListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * CAS-отметки без потерь/дублей; dump отдаёт count/mean и сбрасывает состояние.
 */
public class CheckPointTest {

    private static final long PERIOD = 13L;
    private static final int THREADS = 10;
    private static final int MARKS_PER_THREAD = 1000;

    /** Ручной стаб-слушатель: собирает один дамп-вызов. */
    private static final class CollectingListener implements DataListener {
        long count;
        double mean;
        long currentTime;

        @Override
        public void handle(String metricName, String datasetName, long count, double mean, long currentTime) {
            this.count = count;
            this.mean = mean;
            this.currentTime = currentTime;
        }
    }

    /**
     * N потоков × M отметок в один CheckPoint — CAS без потерь и дублей.
     * Ровно counter = N·M, sum = N·M·period (mean = sum/counter = period). Без sleep.
     */
    @Test
    public void concurrentMark_NothingLost() throws Exception {
        final CheckPoint checkPoint = new CheckPoint();
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(THREADS);
        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int j = 0; j < MARKS_PER_THREAD; j++) {
                        checkPoint.mark(PERIOD);
                    }
                    done.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        start.countDown();
        assertTrue("все потоки завершились", done.await(30, TimeUnit.SECONDS));
        for (Thread thread : threads) {
            thread.join(30000);
        }

        CollectingListener listener = new CollectingListener();
        checkPoint.dumpCurrentState(listener, "metric", "dataset");

        assertEquals(THREADS * MARKS_PER_THREAD, listener.count);
        // sum = counter * period, mean = sum / counter = period — точное равенство
        assertEquals(PERIOD, listener.mean, 0.0);
    }

    /**
     * dump отдаёт count и mean = sum/counter и СБРАСЫВАЕТ состояние
     * (повторный dump → 0/NaN, события не дублируются); idle-счётчик → 0/NaN.
     */
    @Test
    public void dump_ResetsState_AndIdleIsZeroNaN() {
        CheckPoint checkPoint = new CheckPoint();
        checkPoint.mark(1L);
        checkPoint.mark(2L);
        checkPoint.mark(3L);
        checkPoint.mark(4L);

        CollectingListener listener = new CollectingListener();
        checkPoint.dumpCurrentState(listener, "metric", "dataset");

        assertEquals(4, listener.count);
        // mean = (1+2+3+4)/4 — среднее по записанным period'ам
        assertEquals(2.5, listener.mean, 0.0);

        // сброс: повторный dump тех же событий — дублей нет
        checkPoint.dumpCurrentState(listener, "metric", "dataset");
        assertEquals(0, listener.count);
        assertTrue(Double.isNaN(listener.mean));

        // idle-счётчик (без отметок) → dump: count=0, mean=NaN.
        // Уборка неактивных ключей — на уровне хранилища (вытеснение + TTL).
        CheckPoint idle = new CheckPoint();
        idle.dumpCurrentState(listener, "metric", "idle-dataset");
        assertEquals(0, listener.count);
        assertTrue(Double.isNaN(listener.mean));
    }
}

package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: stukushin
 * Date: 27.08.13
 * Time: 14:47
 */
public class CheckPoint {

    private String name;
    private DataListener dataListener;

    private long timeout = 1000; // 1 second

    private static final Map<String, Collection<Long>> marks = new ConcurrentHashMap<>();

    private final Timer timer;
    private final TimerTask dumpTimerTask;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private static final Object monitor = new Object();

    class MarkTask implements Callable<Void> {

        private String actorId;
        private long period;

        MarkTask(String actorId, long period) {
            this.actorId = actorId;
            this.period = period;
        }

        @Override
        public Void call() throws Exception {

            synchronized (monitor) {
                Collection<Long> periods = marks.get(actorId);

                if (periods == null) {
                    periods = new ArrayList<>();
                }

                periods.add(period);

                marks.put(actorId, periods);
            }

            return null;
        }
    }

    class DumpTimerTask extends TimerTask {
        @Override
        public void run() {
            Set<Map.Entry<String, Collection<Long>>> entries;

            synchronized (monitor) {
                if (marks.isEmpty()) {
                    return;
                }

                entries = new HashSet<>(marks.entrySet());

                marks.clear();
            }

            for (Map.Entry<String, Collection<Long>> entry : entries) {
                dataListener.handle(CheckPoint.class.getSimpleName(), name, entry.getKey(), getAverageValue(entry.getValue()), System.currentTimeMillis());
            }
        }

        private long getAverageValue(Collection<Long> periods) {
            long sum = 0;

            for (long period : periods) {
                sum += period;
            }

            return sum / periods.size();
        }
    }

    public CheckPoint(String name, DataListener dataListener) {
        this.name = name;
        this.dataListener = dataListener;

        timer = new Timer("CheckPointDumper");
        dumpTimerTask = new DumpTimerTask();
        timer.schedule(dumpTimerTask, 0, timeout);
    }

    public void mark(String actorId, long period) {
        executorService.submit(new MarkTask(actorId, period));
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void shutdown() {
        dumpTimerTask.cancel();
        timer.cancel();
    }
}

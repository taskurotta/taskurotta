package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private long lastDumpTime = System.currentTimeMillis();

    private final Map<String, Collection<Long>> marks = new ConcurrentHashMap<>();

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    public CheckPoint(String name, DataListener dataListener) {
        this.name = name;
        this.dataListener = dataListener;
    }

    class MarkTask implements Callable<Void> {

        private String actorId;
        private long period;

        MarkTask(String actorId, long period) {
            this.actorId = actorId;
            this.period = period;
        }

        @Override
        public Void call() throws Exception {
            synchronized (marks) {
                Collection<Long> periods = marks.get(actorId);

                if (periods == null) {
                    periods = new ArrayList<>();
                    marks.put(actorId, periods);
                }

                periods.add(period);
            }

            dump();

            return null;
        }

        private void dump() {
            if (System.currentTimeMillis() - lastDumpTime < timeout) {
                return;
            }

            lastDumpTime = System.currentTimeMillis();

            Set<Map.Entry<String, Collection<Long>>> entries;
            synchronized (marks) {
                entries = new HashSet<>(marks.entrySet());
                marks.clear();
            }

            for (Map.Entry<String, Collection<Long>> entry : entries) {
                dataListener.handle(name, entry.getKey(), getAverageValue(entry.getValue()), System.currentTimeMillis());
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

    public void mark(String actorId, long period) {
        executorService.submit(new MarkTask(actorId, period));
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}

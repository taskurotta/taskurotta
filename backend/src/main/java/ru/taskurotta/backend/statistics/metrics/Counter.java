package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;

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
 * Time: 13:32
 */
public class Counter {

    private String name;
    private DataListener dataListener;

    private long timeout = 1000; // 1 second

    private static final Map<String, Long> marks = new ConcurrentHashMap<>();

    private final Timer timer;
    private final TimerTask dumpTimerTask;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    class MarkTask implements Callable<Void> {

        private String actorId;

        MarkTask(String actorId) {
            this.actorId = actorId;
        }

        @Override
        public Void call() throws Exception {

            synchronized (marks) {
                Long count = marks.get(actorId);

                if (count == null) {
                    count = 0l;
                }

                marks.put(actorId, count + 1);
            }

            return null;
        }
    }

    class DumpTimerTask extends TimerTask {

        @Override
        public void run() {
            Set<Map.Entry<String, Long>> entries;

            synchronized (marks) {
                if (marks.isEmpty()) {
                    return;
                }

                entries = new HashSet<>(marks.entrySet());

                marks.clear();
            }

            for (Map.Entry<String, Long> entry : entries) {
                dataListener.handle(Counter.class.getSimpleName(), name, entry.getKey(), entry.getValue(), System.currentTimeMillis());
            }
        }
    }

    public Counter(final String name, final DataListener dataListener) {
        this.name = name;
        this.dataListener = dataListener;

        timer = new Timer("CounterDumper");
        dumpTimerTask = new DumpTimerTask();
        timer.schedule(dumpTimerTask, 0, timeout);
    }

    public void mark(String actorId) {
        executorService.submit(new MarkTask(actorId));
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void shutdown() {
        dumpTimerTask.cancel();
        timer.cancel();
    }
}

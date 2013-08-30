package ru.taskurotta.backend.statistics.metrics;

import ru.taskurotta.backend.statistics.datalisteners.DataListener;
import ru.taskurotta.backend.statistics.datalisteners.LoggerDataListener;

import java.util.Map;
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

    private long timeout = 60000; // 1 minute
    private long lastDumpTime = System.currentTimeMillis();

    private final Map<String, Long> marks = new ConcurrentHashMap<>();

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    public static final String NO_NAME_ACTOR = "noNameActor";

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
                    marks.put(actorId, 1l);
                } else {
                    marks.put(actorId, count + 1);
                }

                dump();
            }

            return null;
        }

        private void dump() {
            if (System.currentTimeMillis() - lastDumpTime < timeout) {
                return;
            }

            synchronized (marks) {
                for (Map.Entry<String, Long> entry : marks.entrySet()) {
                    dataListener.handle(name, entry.getKey(), entry.getValue(), System.currentTimeMillis());
                }

                marks.clear();

                lastDumpTime = System.currentTimeMillis();
            }
        }
    }

    public Counter(String name) {
        this.name = name;
        this.dataListener = new LoggerDataListener();
    }

    public Counter(String name, DataListener dataListener) {
        this.name = name;
        this.dataListener = dataListener;
    }

    public void mark(String actorId) {
        executorService.submit(new MarkTask(actorId));
    }

    public void mark() {
        mark(NO_NAME_ACTOR);
    }
}

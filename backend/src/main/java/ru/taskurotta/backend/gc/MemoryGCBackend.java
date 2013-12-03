package ru.taskurotta.backend.gc;

import ru.taskurotta.backend.dependency.links.GraphDao;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskDao;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: stukushin
 * Date: 02.12.13
 * Time: 16:05
 */
public class MemoryGCBackend extends AbstractGCBackend {

    private Map<Long, Set<UUID>> processesForGC = new ConcurrentHashMap<>();

    private Lock lock = new ReentrantLock();

    private long period = 5000;

    private long initialDelay = period;
    private TimeUnit periodTimeUnit = TimeUnit.MILLISECONDS;
    private int poolSize = 1;

    private ExecutorService executorService;

    class ScheduledGCTask implements Runnable {

        @Override
        public void run() {
            if (logger.isTraceEnabled()) {
                logger.trace("Try to find and submit processes to GC at [{}]", new Date());
            }

            Collection<UUID> processIds = getProcessesForGC();
            int size = processIds.size();
            logger.debug("Found [{}] processes for GC", size);

            if (processIds != null && !processIds.isEmpty()) {
                for(final UUID processId: processIds) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            delete(processId);
                        }
                    });
                }
            }

            logger.debug("Found and submitted [{}] processes for GC", size);
        }
    }

    public MemoryGCBackend(ProcessBackend processBackend, GraphDao graphDao, TaskDao taskDao) {
        super(processBackend, graphDao, taskDao);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(poolSize, new ThreadFactory() {

            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("GC-" + counter++);
                return thread;
            }

        });
        scheduledExecutorService.scheduleAtFixedRate(new ScheduledGCTask(), initialDelay, period, periodTimeUnit);

        executorService = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void addProcessToGC(UUID processId, long deleteTime) {

        logger.trace("Try to add process [{}] with keep time [{}] to GC", processId, deleteTime);

        long key = getKey(deleteTime);

        Set<UUID> processIds = processesForGC.get(key);

        if (processIds == null) {
            try {
                lock.lock();

                processIds = processesForGC.get(key);

                if (processIds == null) {
                    processIds = new CopyOnWriteArraySet<>();
                    processesForGC.put(key, processIds);
                }
            } finally {
                lock.unlock();
            }
        }

        processIds.add(processId);
    }

    @Override
    public Collection<UUID> getProcessesForGC() {

        Set<UUID> processIds = new HashSet<>();

        long gcKey = getKey(System.currentTimeMillis());
        Set<Long> keys = new TreeSet<>(processesForGC.keySet());

        for (Long key : keys) {

            if (key <= gcKey) {

                try {
                    lock.lock();

                    processIds.addAll(processesForGC.remove(key));
                } finally {
                    lock.unlock();
                }

            } else {
                break;
            }

        }

        return processIds;
    }

    private long getKey(long time) {
        return time - time % period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public void setPeriodTimeUnit(TimeUnit periodTimeUnit) {
        this.periodTimeUnit = periodTimeUnit;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
}

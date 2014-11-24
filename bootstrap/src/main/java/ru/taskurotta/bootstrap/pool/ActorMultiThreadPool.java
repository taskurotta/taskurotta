package ru.taskurotta.bootstrap.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.ActorExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Actor execution thread pool abstraction.
 * Contains thread pooling/shrinking/expanding logic for a given actor
 *
 * User: dimadin, stukushin
 * Date: 24.04.13
 * Time: 16:14
 */
public class ActorMultiThreadPool implements ActorThreadPool {
    private static final Logger logger = LoggerFactory.getLogger(ActorMultiThreadPool.class);

    private String actorClassName; //actor class served by this pool
    private String taskList;
    private int size = 0; //pool size
    private long shutdownTimeoutMillis;

    private ActorExecutor actorExecutor; //ActorExecutor task instance for the pool
    private AtomicInteger activeActorExecutorThreadCount = new AtomicInteger();

    private ConcurrentHashMap<String, Thread> threadMap;

    public ActorMultiThreadPool(String actorClassName, String taskList, int size, long shutdownTimeoutMillis) {
        this.actorClassName = actorClassName;
        this.taskList = taskList;
        this.size = size;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;

        this.threadMap = new ConcurrentHashMap<String, Thread>(size);
    }

    public synchronized void start(ActorExecutor actorExecutor) {
        this.actorExecutor = actorExecutor;

        for (int i = 0; i < size; i++) {
            createActorExecutorThread();
        }
    }

    /**
     * Shuts current thread of ActorExecutor if there are active others in pool.
     * Should always leave at least one active thread for polling taskServer.
     *
     * Return false if already muted
     *
     * @return boolean
     */
    public synchronized boolean mute() {
        if (logger.isTraceEnabled()) {
            logger.trace("Try to stop actor [{}]'s thread [{}]", actorClassName, Thread.currentThread().getName());
        }

        if (activeActorExecutorThreadCount.get() == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("Only one active actor [{}]'s thread [{}]", actorClassName, Thread.currentThread().getName());
            }

            return false;
        }

        destroyActorExecutorThread();

        if (logger.isTraceEnabled()) {
            logger.trace("Actor [{}]'s has [{}] active threads", actorClassName, activeActorExecutorThreadCount.get());
        }

        return true;
    }

    /**
     * Submits new task to the pool, expanding it to max size. (meaning task server is now active and actors ready for full scale execution).
     */
    public synchronized void wake() {

        int threadsToStart = size - activeActorExecutorThreadCount.get();

        if (threadsToStart == 0) {
            if (logger.isTraceEnabled()) {
                logger.trace("All actor [{}]'s threads [{}] already started", actorClassName, activeActorExecutorThreadCount.get());
            }

            return;
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Try to start [{}] actor [{}]'s threads. Now active [{}]", threadsToStart, actorClassName, activeActorExecutorThreadCount.get());
            }
        }

        for (int i = 0; i < threadsToStart; i++) {
            createActorExecutorThread();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Actor [{}]'s [{}] threads started, [{}] active now", actorClassName, threadsToStart, activeActorExecutorThreadCount.get());
        }
    }

    /**
     * Gracefully shutdown pool
     */
    public synchronized void shutdown() {
        if (logger.isInfoEnabled()) {
            logger.info("Start gracefully shutdown pool for actor [{}]. Maximum shutdown timeout [{}] seconds", actorClassName, shutdownTimeoutMillis / 1000);
        }

        actorExecutor.stopInstance();
        actorExecutor.stopThread();

        long stopTime = System.currentTimeMillis();

        try {
            while (!threadMap.isEmpty()) {

                if (System.currentTimeMillis() - stopTime >= shutdownTimeoutMillis) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Wait [{}] seconds while actor [{}]'s thread pool die, but now exit", (System.currentTimeMillis() - stopTime) / 1000, actorClassName);
                    }

                    return;
                }

                for (Map.Entry<String, Thread> entry : threadMap.entrySet()) {

                    String threadName = entry.getKey();
                    Thread thread = entry.getValue();

                    if (thread == null) {

                        logger.warn("Thread [{}] not exists", threadName);

                        threadMap.remove(threadName);

                        continue;
                    }

                    if (thread.isAlive()) {

                        if (logger.isDebugEnabled()) {
                            logger.debug("Thread [{}] still alive for [{}] seconds after start shutdown", threadName, (System.currentTimeMillis() - stopTime) / 1000);
                        }

                    } else {

                        threadMap.remove(threadName, thread);

                        if (logger.isDebugEnabled()) {
                            logger.debug("Thread [{}] exit after [{}] seconds after start shutdown", threadName, (System.currentTimeMillis() - stopTime) / 1000);
                        }

                    }
                }

                TimeUnit.SECONDS.sleep(10);
            }

            if (logger.isInfoEnabled()) {
                logger.info("Successfully shutdown thread pool for actor [{}] after [{}] seconds", actorClassName, (System.currentTimeMillis() - stopTime) / 1000);
            }
        } catch (Throwable t) {
            logger.error("Throw exception while try to gracefully shutdown thread pool for actor [" + actorClassName + "]", t);
            // just exit
        }
    }

    public int getCurrentSize() {
        return activeActorExecutorThreadCount.get();
    }

    private void createActorExecutorThread() {

        int counter = activeActorExecutorThreadCount.getAndIncrement();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String threadName = actorClassName + (taskList == null ? "" : "[" + taskList + "]") + "-(" + simpleDateFormat.format(new Date()) + ")-" + counter;

        Thread thread = new Thread(actorExecutor, threadName);
        thread.start();

        threadMap.put(threadName, thread);

        if (logger.isTraceEnabled()) {
            logger.trace("Start actor [{}]'s thread [{}]", actorClassName, threadName);
        }
    }

    private void destroyActorExecutorThread() {

        String threadName = Thread.currentThread().getName();

        if (logger.isTraceEnabled()) {
            logger.trace("Try to destroy actor's [{}] thread [{}]", actorClassName, threadName);
        }

        activeActorExecutorThreadCount.decrementAndGet();
        actorExecutor.stopThread();
    }
}

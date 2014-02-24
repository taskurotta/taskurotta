package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
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
public class ActorThreadPool {
    private static final Logger logger = LoggerFactory.getLogger(ActorThreadPool.class);

    private Class actorClass;//actor class served by this pool
    private int size = 0;//pool size
    private long sleepTimeoutMillis;
    private long shutdownTimeoutMillis;

    private ActorExecutor actorExecutor;//ActorExecutor task instance for the pool
    private AtomicInteger activeActorExecutorThreadCount = new AtomicInteger();

    private ConcurrentHashMap<String, Thread> threadMap;

    public ActorThreadPool(Class actorClass, int size, long sleepTimeoutMillis, long shutdownTimeoutMillis) {
        this.actorClass = actorClass;
        this.size = size;
        this.sleepTimeoutMillis = sleepTimeoutMillis;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;

        this.threadMap = new ConcurrentHashMap<String, Thread>(size);
    }

    public void start(ActorExecutor actorExecutor) {
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
            logger.trace("Try to stop actor [{}]'s thread [{}]", actorClass.getName(), Thread.currentThread().getName());
        }

        if (activeActorExecutorThreadCount.get() == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("Only one active actor [{}]'s thread [{}]", actorClass.getName(), Thread.currentThread().getName());
            }

            return false;
        }

        destroyActorExecutorThread();

        if (logger.isTraceEnabled()) {
            logger.trace("Actor [{}]'s has [{}] active threads", actorClass.getSimpleName(), activeActorExecutorThreadCount.get());
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
                logger.trace("All actor [{}]'s threads [{}] already started", actorClass.getName(), activeActorExecutorThreadCount.get());
            }

            return;
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Try to start [{}] actor [{}]'s threads. Now active [{}]", threadsToStart, actorClass.getName(), activeActorExecutorThreadCount.get());
            }
        }

        for (int i = 0; i < threadsToStart; i++) {
            createActorExecutorThread();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Actor [{}]'s [{}] threads started, [{}] active now", actorClass.getName(), threadsToStart, activeActorExecutorThreadCount.get());
        }
    }

    /**
     * Gracefully shutdown pool
     */
    public void shutdown() {
        if (logger.isInfoEnabled()) {
            logger.info("Start gracefully shutdown pool for actor [{}]. Maximum shutdown timeout [{}] seconds", actorClass.getName(), shutdownTimeoutMillis / 1000);
        }

        actorExecutor.stopInstance();

        ThreadLocal<Boolean> hasAlive = new ThreadLocal<Boolean>(){
            @Override
            protected Boolean initialValue() {
                return Boolean.TRUE;
            }
        };

        ThreadLocal<Long> startTime = new ThreadLocal<Long>(){
            @Override
            protected Long initialValue() {
                return System.currentTimeMillis();
            }
        };

        try {
            while (hasAlive.get()) {
                for (Thread thread : threadMap.values()) {
                    if (thread.isAlive()) {
                        if (System.currentTimeMillis() - startTime.get() >= shutdownTimeoutMillis) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Wait [{}] seconds while actor [{}]'s thread [{}] die, but now exit", (System.currentTimeMillis() - startTime.get()) / 1000, actorClass.getName(), thread.getName());
                            }

                            return;
                        }

                        if (logger.isTraceEnabled()) {
                            logger.trace("Actor [{}]'s thread [{}] is alive, wait util thread dies", actorClass.getName(), thread.getName());
                        }

                        TimeUnit.MILLISECONDS.sleep(sleepTimeoutMillis);
                        hasAlive.set(Boolean.TRUE);
                        break;
                    }

                    hasAlive.set(Boolean.FALSE);
                }
            }

            if (logger.isInfoEnabled()) {
                logger.info("Successfully shutdown pool for actor [{}] after [{}] seconds", actorClass.getName(), (System.currentTimeMillis() - startTime.get()) / 1000);
            }
        } catch (InterruptedException e) {
            logger.error("Throw exception while try to gracefully shutdown actor [" + actorClass.getName() + "]", e);
            // just exit
        }
    }

    public int getCurrentSize() {
        return activeActorExecutorThreadCount.get();
    }

    private void createActorExecutorThread() {

        int counter = activeActorExecutorThreadCount.getAndIncrement();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String threadName = actorClass.getSimpleName() + "-(" + simpleDateFormat.format(new Date()) + ")-" + counter;

        Thread thread = new Thread(actorExecutor, threadName);
        thread.setDaemon(true);
        thread.start();

        threadMap.put(threadName, thread);

        if (logger.isTraceEnabled()) {
            logger.trace("Start actor [{}]'s thread [{}]", actorClass.getName(), threadName);
        }
    }

    private void destroyActorExecutorThread() {

        String threadName = Thread.currentThread().getName();

        if (logger.isTraceEnabled()) {
            logger.trace("Try to destroy actor's [{}] thread [{}]", actorClass.getName(), threadName);
        }

        activeActorExecutorThreadCount.decrementAndGet();
        actorExecutor.stopThread();
    }
}

package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    private Thread[] actorExecutorThreads;

    public ActorThreadPool(Class actorClass, int size, long sleepTimeoutMillis, long shutdownTimeoutMillis) {
        this.actorClass = actorClass;
        this.size = size;
        this.sleepTimeoutMillis = sleepTimeoutMillis;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;

        this.actorExecutorThreads = new Thread[size];
        this.activeActorExecutorThreadCount.set(size);
    }

    public void start(ActorExecutor actorExecutor) {
        this.actorExecutor = actorExecutor;

        for (int i = 0; i < size; i++) {
            createActorExecutorThread(i);
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

        if (logger.isTraceEnabled()) {
            logger.trace("Stopping actor [{}]'s thread [{}]", actorClass.getName(), Thread.currentThread().getName());
        }

        actorExecutor.stopThread();
        activeActorExecutorThreadCount.decrementAndGet();

        if (logger.isTraceEnabled()) {
            logger.trace("Actor [{}]'s has [{}] active threads", actorClass.getSimpleName(), activeActorExecutorThreadCount.get());
        }

        return true;
    }

    /**
     * Submits new task to the pool, expanding it to max size. (meaning task server is now active and actors ready for full scale execution).
     */
    public synchronized void wake() {
        if (activeActorExecutorThreadCount.get() == size) {
            if (logger.isTraceEnabled()) {
                logger.trace("All actor [{}]'s threads [{}] already started", actorClass.getName(), activeActorExecutorThreadCount.get());
            }

            return;
        }

        if (actorExecutorThreads == null || actorExecutorThreads.length == 0) {
            throw new RuntimeException("Pool has not been initialized");
        }

        int count = 0;
        for (int i = 0; i < size; i++) {
            if (actorExecutorThreads[i].isAlive()) {
                continue;
            }

            createActorExecutorThread(i);
            count++;
        }

        activeActorExecutorThreadCount.set(size);

        if (logger.isDebugEnabled()) {
            logger.debug("Actor [{}]'s [{}] threads started, [{}] active now", actorClass.getName(), count, activeActorExecutorThreadCount.get());
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
                for (Thread thread : actorExecutorThreads) {
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

    private void createActorExecutorThread(int i) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        Thread thread = new Thread(actorExecutor, actorClass.getSimpleName() + "-(" + simpleDateFormat.format(new Date()) + ")-" + i);
        actorExecutorThreads[i] = thread;
        thread.start();

        if (logger.isTraceEnabled()) {
            logger.trace("Start actor [{}]'s thread [{}]", actorClass.getName(), thread.getName());
        }
    }
}

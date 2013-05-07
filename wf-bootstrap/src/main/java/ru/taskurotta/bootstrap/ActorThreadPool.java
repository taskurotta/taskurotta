package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

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

    public static final int SHUTDOWN_TIMEOUT = 90 * 1000;
    public static final int SLEEP_TIMEOUT = 1;

    private Class actorClass;//actor class served by this pool
    private int size = 0;//pool size
    private ActorExecutor actorExecutor;//ActorExecutor task instance for the pool
    private volatile int activeActorExecutorThreadCount;

    private Thread[] actorExecutorThreads;

    public ActorThreadPool(final Class actorClass, int size) {
        this.actorClass = actorClass;
        this.size = size;

        actorExecutorThreads = new Thread[size];
    }

    public void start(ActorExecutor actorExecutor) {
        this.actorExecutor = actorExecutor;

        for (int i = 0; i < size; i++) {
            Thread thread = new Thread(actorExecutor, actorClass.getSimpleName() + "-" + i);
            actorExecutorThreads[i] = thread;
            thread.start();

            logger.trace("Start actor [{}]'s thread [{}]", actorClass.getName(), thread.getName());
        }

        activeActorExecutorThreadCount = size;
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
        logger.trace("Try to stop actor [{}]'s thread [{}]", actorClass.getName(), Thread.currentThread().getName());

        if (activeActorExecutorThreadCount == 1) {
            logger.debug("Only one active actor [{}]'s thread [{}]", actorClass.getName(), Thread.currentThread().getName());
            return false;
        }

        logger.trace("Stopping actor [{}]'s thread[{}]", actorClass.getName(), Thread.currentThread().getName());
        actorExecutor.stopThread();
        activeActorExecutorThreadCount--;
        logger.trace("Actor [{}]'s has [{}] active threads", actorClass.getSimpleName(), activeActorExecutorThreadCount);

        return true;
    }

    /**
     * Submits new task to the pool, expanding it to max size. (meaning task server is now active and actors ready for full scale execution).
     */
    public synchronized void wake() {
        if (activeActorExecutorThreadCount == size) {
            logger.trace("All actor [{}]'s threads [{}] already started", actorClass.getName(), activeActorExecutorThreadCount);
            return;
        }

        if (actorExecutorThreads == null || actorExecutorThreads.length == 0) {
            throw new RuntimeException("Pool has not been initialized");
        }

        int count = 0;
        for (Thread thread : actorExecutorThreads) {
            if (thread.isAlive()) {
                continue;
            }

            thread.start();
            count++;
            logger.trace("Start actor [{}]'s thread [{}]", actorClass.getName(), thread.getName());
        }

        activeActorExecutorThreadCount = size;
        logger.debug("Actor [{}]'s [{}] threads added, [{}] active now", actorClass.getName(), count, activeActorExecutorThreadCount);
    }

    /**
     * Gracefully shutdown pool
     */
    public void shutdown() {
        logger.info("Start gracefully shutdown pool for actor [{}]", actorClass.getName());

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
                        if (System.currentTimeMillis() - startTime.get() >= SHUTDOWN_TIMEOUT) {
                            logger.warn("Wait [{}] seconds while actor [{}]'s thread [{}] die, but now exit", (System.currentTimeMillis() - startTime.get()) / 1000, actorClass.getName(), thread.getName());
                            return;
                        }

                        logger.trace("Actor [{}]'s thread [{}] is alive, wait util thread dies", actorClass.getName(), thread.getName());

                        TimeUnit.SECONDS.sleep(SLEEP_TIMEOUT);
                        hasAlive.set(Boolean.TRUE);
                        break;
                    }

                    hasAlive.set(Boolean.FALSE);
                }
            }

            logger.info("Successfully shutdown pool for actor [{}]", actorClass.getName());
        } catch (InterruptedException e) {
            logger.error("Throw exception while try to gracefully shutdown actor [" + actorClass.getName() + "]", e);
            // just exit
        }
    }
}

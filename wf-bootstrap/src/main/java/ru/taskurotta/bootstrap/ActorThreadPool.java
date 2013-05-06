package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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

    private Class actorClass;//actor class served by this pool
    private int size = 0;//pool size
    private ActorExecutor actorExecutor;//ActorExecutor task instance for the pool
    private volatile int activeActorExecutorThreadCount;

    private List<Thread> actorExecutorThreads = new ArrayList<>();

    public ActorThreadPool(final Class actorClass, int size) {
        this.actorClass = actorClass;
        this.size = size;
    }

    public void start(ActorExecutor actorExecutor) {
        this.actorExecutor = actorExecutor;

        for (int i = 0; i < size; i++) {
            startActorExecutorThread();
        }

        activeActorExecutorThreadCount = size;
    }


    //Shuts current thread of ActorExecutor if there are active others in pool. Should always leave at least one active
    //thread for polling taskServer
    //returns false if already muted
    public synchronized boolean mute() {
        if (activeActorExecutorThreadCount == 1) {
            logger.debug("Only one active actor [{}]'s thread [{}]", actorClass, Thread.currentThread().getName());
            return false;
        }

        logger.debug("Stopping actor [{}]'s thread[{}]", actorClass, Thread.currentThread().getName());
        actorExecutor.stopThread();
        activeActorExecutorThreadCount--;

        return true;
    }

    //Submits new task to the pool, expanding it to max size. (meaning task server is now active and actors ready for full scale execution)
    public synchronized void wake() {
        int canBeExecuted = size - activeActorExecutorThreadCount;
        if(canBeExecuted > 0) {
            for(int i = 0; i < canBeExecuted; i++) {
                startActorExecutorThread();
            }

            logger.debug("Actor[{}]'s [{}] threads added, [{}] active now", actorClass, canBeExecuted, activeActorExecutorThreadCount);
        }
    }

    public void shutdown() {
        logger.info("Start gracefully shutdown ActorThreadPool for actor [{}]", actorClass);

        actorExecutor.stopInstance();

        boolean hasAlive = true;
        int timeout = 90 * 1000; // seconds
        ThreadLocal<Long> startTime = new ThreadLocal<Long>(){
            @Override
            protected Long initialValue() {
                return System.currentTimeMillis();
            }
        };

        while (hasAlive) {
            for (Thread thread : actorExecutorThreads) {
                if (thread.isAlive()) {
                    if (System.currentTimeMillis() - startTime.get() >= timeout) {
                        logger.debug("Wait [{}] seconds while thread [{}] die, but now exit", (System.currentTimeMillis() - startTime.get()) / 1000, thread.getName());
                        return;
                    }

                    logger.trace("Thread [{}] is alive, wait util thread dies", thread.getName());

                    try {
                        TimeUnit.SECONDS.sleep(1);
                        hasAlive = true;
                        break;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                hasAlive = false;
            }
        }
    }

    private void startActorExecutorThread() {
        activeActorExecutorThreadCount++;
        Thread thread = new Thread(actorExecutor, actorClass.getSimpleName() + "-" + activeActorExecutorThreadCount);
        actorExecutorThreads.add(thread);
        thread.start();

        logger.trace("Start actor [{}]'s thread [{}]", actorClass, thread.getName());
    }
}

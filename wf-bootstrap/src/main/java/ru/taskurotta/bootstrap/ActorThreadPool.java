package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Actor execution thread pool abstraction.
 * Contains thread pooling/shrinking/expanding logic for a given actor
 *
 * User: dimadin
 * Date: 24.04.13
 * Time: 16:14
 */
public class ActorThreadPool {
    private static final Logger logger = LoggerFactory.getLogger(ActorThreadPool.class);

    private ThreadPoolExecutor poolService;
    private Class actorClass;//actor class served by this pool
    private int size = 0;//pool size
    private ActorExecutor actorExecutor;//ActorExecutor task instance for the pool

    //Initial run of the job. Starts only one thread, which would wake the whole pool on (if) next success iteration
    public void startExecution(ActorExecutor actorExecutor) {
        this.actorExecutor = actorExecutor;
        poolService.execute(actorExecutor);
        logger.debug("ActorThreadPool sized[{}], started for actor[{}]", size, actorClass);

        if (logger.isDebugEnabled()) {
            Thread monitor = new Thread(){
                @Override
                public void run() {
                    while(true) {
                        try {
                            Thread.sleep(30000l);
                            logger.debug("Pool monitor for [{}], active threads[{}]", actorClass, poolService.getActiveCount());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            monitor.setDaemon(true);
            monitor.start();
        }
    }

    public ActorThreadPool(final Class actorClass, int size) {
        this.actorClass = actorClass;
        this.size = size;
        poolService = new ThreadPoolExecutor(size, size, 0l, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            private int count;
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, actorClass.getSimpleName()+"-"+ (++count));
            }
        });
    }

    //Shuts current thread of ActorExecutor if there are active others in pool. Should always leave at least one active
    //thread for polling taskServer
    //returns false if already muted
    public synchronized  boolean muteThreadPool() {
        boolean result = false;
        if(poolService.getActiveCount() > 1) {
            logger.debug("Stopping actor [{}]'s thread[{}]", actorClass, Thread.currentThread().getName());
            actorExecutor.stop();
            result = true;
        }
        return result;
    }

    //Submits new task to the pool, expanding it to max size. (meaning task server is now active and actors ready for full scale execution)
    public synchronized void wakeThreadPool() {
        int canBeExecuted = size - poolService.getActiveCount();
        if(canBeExecuted > 0) {
            for(int i = 0; i < canBeExecuted; i++) {
                poolService.execute(actorExecutor);
            }
            logger.debug("Actor[{}]'s threadpool has been waked. [{}] threads added, [{}] active now", actorClass, canBeExecuted, poolService.getActiveCount());
        }
    }

    public void shutdown() {
        poolService.shutdown();
    }

}

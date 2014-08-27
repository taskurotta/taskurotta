package ru.taskurotta.bootstrap.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.ActorExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created on 27.08.2014.
 */
public class ActorSingleThreadPool implements ActorThreadPool {

    private static final Logger logger = LoggerFactory.getLogger(ActorSingleThreadPool.class);

    private String actorClassName;
    private String taskList;
    private long shutdownTimeoutMs;
    private ActorExecutor actorExecutor;
    private Thread thread;


    public ActorSingleThreadPool(String actorClassName, String taskList, long shutdownTimeoutMs) {
        this.actorClassName = actorClassName;
        this.taskList = taskList;
        this.shutdownTimeoutMs = shutdownTimeoutMs;
    }

    @Override
    public void start(ActorExecutor actorExecutor) {
        this.actorExecutor = actorExecutor;
        String dateMark = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date());
        String threadName = actorClassName + (taskList == null ? "" : "[" + taskList + "]") + "-(" + dateMark + ")";

        this.thread = new Thread(actorExecutor, threadName);
        thread.start();

        logger.trace("Start actor [{}]'s thread [{}]", actorClassName, threadName);
    }

    @Override
    public boolean mute() {
        return false;
    }

    @Override
    public void wake() {
        //do nothing
    }

    @Override
    public void shutdown() {
        if (this.thread != null && this.thread.isAlive()) {
            logger.info("Start gracefully shutdown pool for actor [{}]. Maximum shutdown timeout [{}] seconds", actorClassName, shutdownTimeoutMs / 1000);

            long start = System.currentTimeMillis();
            actorExecutor.stopInstance();
            actorExecutor.stopThread();

            while (this.thread.isAlive()) {
                if (System.currentTimeMillis() - start >= shutdownTimeoutMs) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Terminated graceful shutdown await for actor[{}] thread due to timeout: [{}]ms. ", actorClassName, shutdownTimeoutMs);
                    }
                    return;
                }

                try {
                    Thread.sleep(10);//wait for thread to terminate
                } catch (InterruptedException e) {
                    logger.warn("Thread interrupted on shutdown wait", e);
                }
            }

            logger.info("Pool for actor [{}] gracefully shut down in [{}]ms", actorClassName, System.currentTimeMillis()-start);

        }

    }

    @Override
    public int getCurrentSize() {
        return 1;
    }
}

package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.Environment;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.exception.server.ServerConnectionException;
import ru.taskurotta.exception.server.ServerException;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 19:29
 */
public class ActorExecutor implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ActorExecutor.class);

    private Profiler profiler;
    private RuntimeProcessor runtimeProcessor;
    private TaskSpreader taskSpreader;

    private ThreadLocal<Boolean> threadRun = new ThreadLocal<Boolean>();
    private volatile boolean instanceRun = true;

    public ActorExecutor(Profiler profiler, Inspector inspector, RuntimeProcessor runtimeProcessor, TaskSpreader taskSpreader) {
        this.profiler = profiler;
        this.runtimeProcessor = inspector.decorate(profiler.decorate(runtimeProcessor));
        this.taskSpreader = inspector.decorate(profiler.decorate(taskSpreader));
    }

    @Override
    public void run() {

        String threadName = Thread.currentThread().getName();

        logger.trace("Started actor executor thread [{}]", threadName);

        threadRun.set(Boolean.TRUE);

        while (threadRun.get() && instanceRun) {

            profiler.cycleStart();

            try {

                logger.trace("Thread [{}]: try to poll", threadName);
                Task task = taskSpreader.poll();

                if (task == null) {
                    continue;
                }

                logger.trace("Thread [{}]: try to execute task [{}]", threadName, task);
                TaskDecision taskDecision = runtimeProcessor.execute(task);

                logger.trace("Thread [{}]: try to release decision [{}] of task [{}]", threadName, taskDecision, task);
                taskSpreader.release(taskDecision);

            } catch (ServerConnectionException ex) {
                logger.error("Connection to task server error. {}: {}", ex.getCause().getClass(), ex.getMessage());
            } catch (ServerException ex) {
                logger.error("Error at client-server communication", ex);
            } catch (Throwable t) {
                logger.error("Unexpected actor execution error", t);
                if (Environment.getInstance().getType() == Environment.Type.TEST) {
                    throw new RuntimeException(t);
                }
            } finally {
                profiler.cycleFinish();
            }

        }

        logger.trace("Finish actor executor thread [{}]", threadName);
    }

    /**
     * stop current thread
     */
    void stopThread() {
        threadRun.set(Boolean.FALSE);

        if (logger.isDebugEnabled()) {
            logger.debug("Set threadRun = false for thread [{}]", Thread.currentThread().getName());
        }
    }

    /**
     * stop all threads
     */
    void stopInstance() {
        instanceRun = false;

        if (logger.isDebugEnabled()) {
            logger.debug("Set instanceRun = false for thread from thread [{}]", Thread.currentThread().getName());
        }
    }
}


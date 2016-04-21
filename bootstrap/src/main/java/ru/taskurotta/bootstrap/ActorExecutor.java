package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.Environment;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.exception.SerializationException;
import ru.taskurotta.exception.server.ServerConnectionException;
import ru.taskurotta.exception.server.ServerException;
import ru.taskurotta.internal.Heartbeat;
import ru.taskurotta.internal.TaskUID;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.util.DuplicationErrorSuppressor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 19:29
 */
public class ActorExecutor implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ActorExecutor.class);

    private DuplicationErrorSuppressor duplicationErrorSuppressor = new DuplicationErrorSuppressor();

    private Profiler profiler;
    private RuntimeProcessor runtimeProcessor;
    private TaskSpreader taskSpreader;

    private ThreadLocal<Boolean> threadRun = new ThreadLocal<Boolean>();
    private volatile boolean instanceRun = true;
    private ConcurrentHashMap<TaskUID, Long> timeouts;
    private long sleepOnServerErrorMilliseconds;

    public ActorExecutor(Profiler profiler, Inspector inspector, RuntimeProcessor runtimeProcessor,
                         TaskSpreader taskSpreader, ConcurrentHashMap<TaskUID, Long> timeouts,
                         long sleepOnServerErrorMilliseconds) {
        this.profiler = profiler;
        this.runtimeProcessor = inspector.decorate(profiler.decorate(runtimeProcessor));
        this.taskSpreader = inspector.decorate(profiler.decorate(taskSpreader));
        this.timeouts = timeouts;
        this.sleepOnServerErrorMilliseconds = sleepOnServerErrorMilliseconds;
    }

    @Override
    public void run() {

        String threadName = Thread.currentThread().getName();

        logger.trace("Started actor executor thread [{}]", threadName);

        threadRun.set(Boolean.TRUE);


        boolean sleepOnFail = false;

        while (threadRun.get() && instanceRun) {

            profiler.cycleStart();

            try {

                logger.trace("Thread [{}]: try to poll", threadName);
                Task task = null;

                try {

                    sleepOnFail = true;
                    task = taskSpreader.poll();
                } catch (SerializationException ex) {

                    UUID taskId = ex.getTaskId();

                    if (taskId == null) {
                        throw ex;
                    }

                    // send error decision to the server
                    Throwable realEx = ex.getCause();
                    if (realEx == null) {
                        realEx = ex;
                    }

                    TaskDecision errorDecision = new TaskDecisionImpl(taskId, ex.getProcessId(), ex.getPass(), realEx,
                            null);

                    logger.error("Can not deserialize task. Try to release error decision [{}]", errorDecision);

                    sleepOnFail = true;
                    taskSpreader.release(errorDecision);
                    continue;
                }

                if (task == null) {
                    continue;
                }

                logger.trace("Thread [{}]: try to execute task [{}]", threadName, task);

                TaskDecision taskDecision = null;

                final TaskUID taskUID = new TaskUID(task.getId(), task.getProcessId());
                try {
                    sleepOnFail = false;
                    taskDecision = runtimeProcessor.execute(task, new Heartbeat() {
                        @Override
                        public void updateTimeout(long timeout) {
                            timeouts.put(taskUID, timeout);
                        }
                    });
                } catch (SerializationException ex) {

                    UUID taskId = ex.getTaskId();

                    if (taskId == null) {
                        throw ex;
                    }

                    // send error decision to the server
                    Throwable realEx = ex.getCause();
                    if (realEx == null) {
                        realEx = ex;
                    }

                    TaskDecision errorDecision = new TaskDecisionImpl(taskId, ex.getProcessId(), ex.getPass(), realEx,
                            null);

                    logger.error("Can not serialize task decision. Try to release error decision [{}]", errorDecision);

                    sleepOnFail = true;
                    taskSpreader.release(errorDecision);
                    continue;
                } finally {
                    timeouts.remove(taskUID);
                }

                logger.trace("Thread [{}]: try to release decision [{}] of task [{}]", threadName, taskDecision, task);
                sleepOnFail = true;
                taskSpreader.release(taskDecision);

            } catch (ServerConnectionException ex) {
                logError("Connection to task server error", ex, sleepOnFail);
            } catch (ServerException ex) {
                logError("Error at client-server communication", ex, sleepOnFail);
            } catch (Throwable t) {
                boolean isTest = Environment.getInstance().getType() == Environment.Type.TEST;
                logError("Unexpected actor execution error", t, sleepOnFail && !isTest);
                if (isTest) {
                    throw new RuntimeException(t);
                }
            } finally {
                profiler.cycleFinish();
            }

        }

        logger.debug("Finish actor executor thread [{}]", threadName);
    }

    protected void logError(String msg, Throwable ex, boolean sleepOnFail) {
        if (!duplicationErrorSuppressor.isLastErrorEqualsTo(msg, ex)) {
            logger.error(msg, ex);
        }

        logger.trace("Sleep {} on fail {} milliseconds", sleepOnFail, sleepOnServerErrorMilliseconds);

        if (sleepOnFail) {
            // wait a while before next possible fail
            try {
                TimeUnit.MILLISECONDS.sleep(sleepOnServerErrorMilliseconds);
            } catch (InterruptedException ignore) {
            }
        }
    }

    /**
     * stop current thread
     */
    public void stopThread() {
        threadRun.set(Boolean.FALSE);

        if (logger.isDebugEnabled()) {
            logger.debug("Set threadRun = false for thread [{}]", Thread.currentThread().getName());
        }
    }

    /**
     * stop all threads
     */
    public void stopInstance() {
        instanceRun = false;

        if (logger.isDebugEnabled()) {
            logger.debug("Set instanceRun = false for thread from thread [{}]", Thread.currentThread().getName());
        }
    }
}


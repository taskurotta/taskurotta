package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final static Logger log = LoggerFactory.getLogger(ActorExecutor.class);

    private Profiler profiler;
    private RuntimeProcessor runtimeProcessor;
    private TaskSpreader taskSpreader;

    private ThreadLocal<Boolean> threadRun = new ThreadLocal<>();
    private volatile boolean instanceRun = true;

    public ActorExecutor(Profiler profiler, Inspector inspector, RuntimeProcessor runtimeProcessor, TaskSpreader taskSpreader) {
        this.profiler = profiler;
        this.runtimeProcessor = inspector.decorate(profiler.decorate(runtimeProcessor));
        this.taskSpreader = inspector.decorate(profiler.decorate(taskSpreader));
    }

    @Override
    public void run() {
        log.trace("Started executor thread [{}]", Thread.currentThread().getName());

        threadRun.set(Boolean.TRUE);

        while (threadRun.get() && instanceRun) {

            profiler.cycleStart();

            try {
                log.trace("Poll executor thread [{}]", Thread.currentThread().getName());
                Task task = taskSpreader.poll();

                if (task == null) {
                    profiler.cycleFinish();
                    continue;
                }

                TaskDecision taskDecision = runtimeProcessor.execute(task);

                long start = System.currentTimeMillis();
                taskSpreader.release(taskDecision);
                log.trace("Release took [{}]", (System.currentTimeMillis() - start));
            } catch(ServerConnectionException ex) {
                log.error("Connection to task server error. {}: {}", ex.getCause().getClass(), ex.getMessage());
            } catch(ServerException ex) {
                log.error("Error at client-server communication", ex);
            } catch (Throwable t) {
                log.error("Unexpected actor execution error", t);
            } finally {
                profiler.cycleFinish();
            }

        }

        log.trace("Exit executor thread [{}]", Thread.currentThread().getName());
    }

    /**
     * stop current thread
     */
    void stopThread() {
        log.trace("Set threadRun = false for thread [{}]", Thread.currentThread().getName());
        threadRun.set(Boolean.FALSE);
    }

    /**
     * stop all threads
     */
    public void stopInstance() {
        log.debug("Shutdown ActorExecutor");
        instanceRun = false;
    }
}


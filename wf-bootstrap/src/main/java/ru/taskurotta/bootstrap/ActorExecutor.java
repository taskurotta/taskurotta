package ru.taskurotta.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

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

    private boolean shutdown = false;

    public ActorExecutor(Profiler profiler, Inspector inspector, RuntimeProcessor runtimeProcessor, TaskSpreader taskSpreader) {
        this.profiler = profiler;
        this.runtimeProcessor = inspector.decorate(profiler.decorate(runtimeProcessor));
        this.taskSpreader = inspector.decorate(profiler.decorate(taskSpreader));
    }

    @Override
    public void run() {

        while (!shutdown) {

            profiler.cycleStart();

            try {

                Task task = taskSpreader.poll();

                if (task == null) {
                    profiler.cycleFinish();
                    continue;
                }

                TaskDecision taskDecision = runtimeProcessor.execute(task);

                taskSpreader.release(taskDecision);

            } catch (Throwable t) {
                log.error("Exception caught", t);
            } finally {
                profiler.cycleFinish();
            }

        }
    }

    public void stop() {
        shutdown = true;
    }
}


package ru.taskurotta.bootstrap;

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

            } finally {
                profiler.cycleFinish();
            }

        }
    }

    public void stop() {
        shutdown = true;
    }
}


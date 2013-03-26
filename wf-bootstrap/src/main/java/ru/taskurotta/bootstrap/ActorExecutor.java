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

    boolean shutdown = false;

    public ActorExecutor(Profiler profiler, RuntimeProcessor runtimeProcessor, TaskSpreader taskSpreader) {
        this.profiler = profiler;
        this.runtimeProcessor = profiler.decorate(runtimeProcessor);
        this.taskSpreader = profiler.decorate(taskSpreader);
    }

    @Override
    public void run() {

        while (!shutdown) {

            profiler.cycleStart();

            try {

                Task task = taskSpreader.pull();

                if (task == null) {
                    profiler.cycleFinish();

                    // TODO: sleep one or few seconds? Or implement sleep policy?
                    continue;
                }

                // TODO: catch all exceptions and send it to server
                TaskDecision taskDecision = runtimeProcessor.execute(task);

                taskSpreader.release(taskDecision);

            } finally {
                profiler.cycleFinish();
            }

        }
    }
}


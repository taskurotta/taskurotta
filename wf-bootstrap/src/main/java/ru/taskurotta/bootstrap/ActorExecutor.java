package ru.taskurotta.bootstrap;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.exception.ActorRuntimeException;

import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 19:29
 */
public class ActorExecutor implements Runnable {

    private Meter meter;
    private Timer timer;

    private Profiler profiler;
    private RuntimeProcessor runtimeProcessor;
    private TaskSpreader taskSpreader;

    boolean shutdown = false;

    public ActorExecutor(Profiler profiler, RuntimeProcessor runtimeProcessor, TaskSpreader taskSpreader) {
        this.profiler = profiler;
        this.runtimeProcessor = runtimeProcessor;
        this.taskSpreader = taskSpreader;

    }

    @Override
    public void run() {

        while (!shutdown) {

            profiler.cycleStart();

            profiler.pullStart();
            Task task = taskSpreader.pull();

            if (task == null) {
                profiler.pullFinish(false);
                profiler.cycleFinish(false, false);

                // TODO: sleep one or few seconds? Or implement sleep policy?
                continue;
            }

            profiler.pullFinish(true);


            profiler.executeStart();

            // TODO: catch all exceptions and send it to server
            TaskDecision taskDecision = runtimeProcessor.execute(task);

            profiler.executeFinish(task.getTarget(), false);

            profiler.releaseStart();
            taskSpreader.release(taskDecision);

            profiler.releaseFinish();

            profiler.cycleFinish(true, false);
        }
    }
}


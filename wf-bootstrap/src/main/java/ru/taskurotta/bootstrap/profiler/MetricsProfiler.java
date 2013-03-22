package ru.taskurotta.bootstrap.profiler;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.util.ActorDefinition;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * User: romario
 * Date: 3/22/13
 * Time: 1:37 PM
 */
public class MetricsProfiler implements Profiler {


    // TODO create all timers and use yaml config

    private Meter meterCycle;
    private Timer timerCycle;
    private Timer timerPull;
    private Timer timerExecute;
    private Timer timerRelease;
    private Timer timerError;

    // ?? what is default config values ??

    private boolean isMeterCycle = true;
    private boolean isTrackCycle = false;
    private boolean isTrackPull = false;
    private boolean isTrackExecute = true;
    private boolean isTrackRelease = false;
    private boolean isTrackError = false;

    private ThreadLocal<Long> cycleStartTime = new ThreadLocal<Long>();


    public MetricsProfiler(Class actorClass) {

        ActorDefinition actorDefinition = ActorDefinition.valueOf(actorClass);

        if (isMeterCycle) {
            String meterName = actorDefinition.getFullName() + "#meterCycle";
            meterCycle = Metrics.newMeter(actorClass, meterName, "requests", TimeUnit.SECONDS);
        }

        if (isTrackExecute) {
            String timerName = actorDefinition.getFullName() + "#timerExecute";
            timerExecute = Metrics.newTimer(actorClass, timerName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        }

    }

    @Override
    public RuntimeProcessor decorate(final RuntimeProcessor runtimeProcessor) {

        if (!isTrackExecute) {
            return runtimeProcessor;
        }

        return new RuntimeProcessor() {

            @Override
            public TaskDecision execute(Task task) {

                long startTime = System.nanoTime();

                try {
                    return runtimeProcessor.execute(task);
                } finally {
                    timerExecute.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                }

            }

            @Override
            public List<Task> execute(Runnable runnable) {
                throw new IllegalAccessError("Method not supported yet");
            }
        };

    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {

        if (!(isTrackPull || isTrackRelease || isTrackError)) {
            return taskSpreader;
        }

        return new TaskSpreader() {

            @Override
            public Task pull() {

                if (!isTrackPull) {
                    return taskSpreader.pull();
                }

                long startTime = System.nanoTime();

                try {
                    return taskSpreader.pull();
                } finally {
                    timerPull.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                }

            }

            @Override
            public void release(TaskDecision taskDecision) {

                if (!isTrackPull) {
                    taskSpreader.release(taskDecision);
                    return;
                }

                long startTime = System.nanoTime();

                try {
                    taskSpreader.release(taskDecision);
                } finally {
                    timerRelease.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                }
            }
        };
    }

    @Override
    public void cycleStart() {

        if (isMeterCycle) {
            meterCycle.mark();
        }

        if (!isTrackCycle) {
            return;
        }

        cycleStartTime.set(System.nanoTime());
    }

    @Override
    public void cycleFinish() {

        if (!isTrackCycle) {
            return;
        }

        timerCycle.update(System.nanoTime() - cycleStartTime.get(), TimeUnit.NANOSECONDS);
    }

}

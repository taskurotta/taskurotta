package ru.taskurotta.bootstrap.profiler;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.util.ActorDefinition;

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

    private boolean isTrack = false;

    private ThreadLocal<Times> threadLocalTimes = new ThreadLocal<Times>();

    private static class Times {
        long startCycle;
        long startPull;
        long startExecute;
        long startRelease;
        long startError;
    }

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

        isTrack = isTrackCycle || isTrackPull || isTrackExecute || isTrackRelease || isTrackError;

    }

    @Override
    public void cycleStart() {

        if (isTrack) {
            threadLocalTimes.set(new Times());
        }

        if (isMeterCycle) {
            meterCycle.mark();
        }

        if (!isTrackCycle) {
            return;
        }
    }

    @Override
    public void cycleFinish(boolean withTask, boolean withError) {

        if (!isTrack) {
            return;
        }

        if (isTrackCycle) {
        }

        // should we remove it? or can reuse object late?
        threadLocalTimes.remove();
    }

    @Override
    public void pullStart() {

        if (!isTrackPull) {
            return;
        }
    }

    @Override
    public void pullFinish(boolean withTask) {

        if (!isTrackPull) {
            return;
        }
    }

    @Override
    public void executeStart() {

        if (!isTrackExecute) {
            return;
        }

        Times times = threadLocalTimes.get();
        times.startExecute = System.nanoTime();
    }

    @Override
    public void executeFinish(TaskTarget taskTarget, boolean withError) {

        if (!isTrackExecute) {
            return;
        }

        Times times = threadLocalTimes.get();
        timerExecute.update(System.nanoTime() - times.startExecute, TimeUnit.NANOSECONDS);
    }

    @Override
    public void releaseStart() {

        if (!isTrackRelease) {
            return;
        }
    }

    @Override
    public void releaseFinish() {

        if (!isTrackRelease) {
            return;
        }
    }

    @Override
    public void errorStart() {

        if (!isTrackError) {
            return;
        }
    }

    @Override
    public void errorFinish() {

        if (!isTrackError) {
            return;
        }
    }
}

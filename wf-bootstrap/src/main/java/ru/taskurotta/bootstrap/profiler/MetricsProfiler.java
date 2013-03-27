package ru.taskurotta.bootstrap.profiler;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.ConsoleReporter;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.logback.LoggerReporter;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.util.ActorDefinition;

import java.util.Properties;
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

    // meter task cycle
    private boolean isMeterCycle = true;
    // track RuntimeProcessor
    private boolean isTrackExecute = true;
    // track full Task cycle from pull to release
    private boolean isTrackCycle = true;
    // track task pull
    private boolean isTrackPull = true;
    // track task decision release
    private boolean isTrackRelease = true;
    // track task error
    private boolean isTrackError = true;

    // output metrics to log
    private boolean isOutputToLog = false;
    private int logOutputPeriod = 3;

    // output metrics to console
    private boolean isOutputToConsole = false;
    private int consoleOutputPeriod = 3;

    private ThreadLocal<Long> cycleStartTime = new ThreadLocal<Long>();

    public MetricsProfiler(Class actorClass, Properties properties) {

        parseProperties(properties);

        ActorDefinition actorDefinition = ActorDefinition.valueOf(actorClass);

        if (isMeterCycle) {
            String meterName = actorDefinition.getFullName() + "#meterCycle";
            meterCycle = Metrics.newMeter(actorClass, meterName, "requests", TimeUnit.SECONDS);
        }

        if (isTrackExecute) {
            String timerName = actorDefinition.getFullName() + "#timerExecute";
            timerExecute = Metrics.newTimer(actorClass, timerName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        }

        if (isTrackCycle) {
            String timerName = actorDefinition.getFullName() + "#timerCycle";
            timerCycle = Metrics.newTimer(actorClass, timerName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        }

        if (isTrackPull) {
            String timerName = actorDefinition.getFullName() + "#timerPull";
            timerPull = Metrics.newTimer(actorClass, timerName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        }

        if (isTrackRelease) {
            String timerName = actorDefinition.getFullName() + "#timerRelease";
            timerRelease = Metrics.newTimer(actorClass, timerName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        }

        if (isTrackError) {
            String timerName = actorDefinition.getFullName() + "#timerError";
            timerError = Metrics.newTimer(actorClass, timerName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        }

        if (isOutputToLog) {
            LoggerReporter.enable(logOutputPeriod, TimeUnit.SECONDS);
        }

        if (isOutputToConsole) {
            ConsoleReporter.enable(consoleOutputPeriod, TimeUnit.SECONDS);
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
            public Task[] execute(Runnable runnable) {
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

    private void parseProperties(Properties properties) {
        isMeterCycle = !properties.containsKey("meterCycle") || Boolean.parseBoolean(String.valueOf(properties.get("meterCycle")));
        isTrackExecute = !properties.containsKey("trackExecute") || Boolean.parseBoolean(String.valueOf(properties.get("trackExecute")));
        isTrackCycle = !properties.containsKey("trackCycle") || Boolean.parseBoolean(String.valueOf(properties.get("trackCycle")));
        isTrackPull = !properties.containsKey("trackPull") || Boolean.parseBoolean(String.valueOf(properties.get("trackPull")));
        isTrackRelease = !properties.containsKey("trackRelease") || Boolean.parseBoolean(String.valueOf(properties.get("trackRelease")));
        isTrackError = !properties.containsKey("trackError") || Boolean.parseBoolean(String.valueOf(properties.get("trackError")));

        isOutputToLog = !properties.containsKey("outputToLog") || Boolean.parseBoolean(String.valueOf(properties.get("outputToLog")));
        if (properties.containsKey("logOutputPeriod")) {
            logOutputPeriod = Integer.parseInt(String.valueOf(properties.get("logOutputPeriod")));
        }

        isOutputToConsole = !properties.containsKey("outputToConsole") || Boolean.parseBoolean(String.valueOf(properties.get("outputToConsole")));
        if (properties.containsKey("consoleOutputPeriod")) {
            consoleOutputPeriod = Integer.parseInt(String.valueOf(properties.get("consoleOutputPeriod")));
        }
    }

    public void setMeterCycle(boolean meterCycle) {
        this.isMeterCycle = meterCycle;
    }

    public void setTrackExecute(boolean trackExecute) {
        this.isTrackExecute = trackExecute;
    }

    public void setTrackCycle(boolean trackCycle) {
        this.isTrackCycle = trackCycle;
    }

    public void setTrackPull(boolean trackPull) {
        this.isTrackPull = trackPull;
    }

    public void setTrackRelease(boolean trackRelease) {
        this.isTrackRelease = trackRelease;
    }

    public void setTrackError(boolean trackError) {
        this.isTrackError = trackError;
    }

    public void setOutputToLog(boolean outputToLog) {
        this.isOutputToLog = outputToLog;
    }

    public void setLogOutputPeriod(int logOutputPeriod) {
        this.logOutputPeriod = logOutputPeriod;
    }

    public void setOutputToConsole(boolean outputToConsole) {
        this.isOutputToConsole = outputToConsole;
    }

    public void setConsoleOutputPeriod(int consoleOutputPeriod) {
        this.consoleOutputPeriod = consoleOutputPeriod;
    }
}

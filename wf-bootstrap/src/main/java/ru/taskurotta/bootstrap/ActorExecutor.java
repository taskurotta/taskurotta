package ru.taskurotta.bootstrap;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Worker;
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

    private Class actorClass;
    private RuntimeProcessor runtimeProcessor;
    private TaskSpreader taskSpreader;

    boolean shutdown = false;

    public ActorExecutor(Class actorClass, RuntimeProcessor runtimeProcessor, TaskSpreader taskSpreader) {
        this.actorClass = actorClass;
        this.runtimeProcessor = runtimeProcessor;
        this.taskSpreader = taskSpreader;

        String actorVersion;
        if (actorClass.isAnnotationPresent(Decider.class)) {
            actorVersion = ((Decider) actorClass.getAnnotation(Decider.class)).version();
        } else if (actorClass.isAnnotationPresent(Worker.class)) {
            actorVersion = ((Worker) actorClass.getAnnotation(Worker.class)).version();
        } else {
            throw new ActorRuntimeException(actorClass.getCanonicalName() + "don't have @Decider or @Worker annotation");
        }

        String meterName = actorClass.getCanonicalName() + "#" + actorVersion + "#meter";
        meter = Metrics.newMeter(actorClass, meterName, "requests", TimeUnit.SECONDS);

        String timerName = actorClass.getCanonicalName() + "#" + actorVersion + "#timer";
        timer = Metrics.newTimer(actorClass, timerName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        TimerContext timerContext;

        while (!shutdown) {
            meter.mark();

            Task task = taskSpreader.pull();

            if (task == null) {
                continue;
            }

            timerContext = timer.time();
            TaskDecision taskDecision = runtimeProcessor.execute(task);
            timerContext.stop();

            taskSpreader.release(taskDecision);
        }
    }
}


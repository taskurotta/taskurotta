package ru.taskurotta.test.profiler;

import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 15.06.2015
 * Time: 11:52
 */

public class TimeoutProfiler extends SimpleProfiler {

    private long releaseTimeout;

    public TimeoutProfiler(Class actorClass, Properties properties) {
        if (properties.containsKey("releaseTimeout")) {
            this.releaseTimeout = Long.valueOf(properties.getProperty("releaseTimeout"));
        }
    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {
                return taskSpreader.poll();
            }

            @Override
            public void release(TaskDecision taskDecision) {
                try {
                    TimeUnit.MILLISECONDS.sleep(releaseTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                taskSpreader.release(taskDecision);
            }
        };
    }

}

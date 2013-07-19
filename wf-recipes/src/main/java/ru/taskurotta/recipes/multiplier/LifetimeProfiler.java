package ru.taskurotta.recipes.multiplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by void 12.07.13 18:29
 */
public class LifetimeProfiler extends SimpleProfiler {
    private final static Logger log = LoggerFactory.getLogger(LifetimeProfiler.class);

    private AtomicLong taskCount = new AtomicLong(0);
    private long startTime;
    private long lastTime;
    private boolean exitAfterAll;
    private int tasksForStat = 100;

    public LifetimeProfiler() {
    }

    public LifetimeProfiler(Class actorClass, Properties properties) {
        if (properties.containsKey("exitAfterAll")) {
            exitAfterAll = (Boolean)properties.get("exitAfterAll");
        }

        if (properties.containsKey("tasksForStat")) {
            tasksForStat = Integer.parseInt(properties.getProperty("tasksForStat", "100"));
        }
    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {
                if (taskCount.get() == 0) {
                    lastTime = startTime = System.currentTimeMillis();
                }

                Task task = taskSpreader.poll();

                long curTime = System.currentTimeMillis();
                if (null != task) {
                    long count = taskCount.incrementAndGet();
                    if (count % tasksForStat == 0) {
                        System.out.printf("       tasks: %6d; time: %6.3f; rate: %8.3f\n", count, 0.001 * (curTime - lastTime), 1000.0D * tasksForStat / (curTime - lastTime));
                        lastTime = curTime;
                    }
                } else {
                    System.out.printf("TOTAL: tasks: %6d; time: %6.3f; rate: %8.3f\n", taskCount.get(), 1.0 * (lastTime - startTime) / 1000.0, 1000.0 * taskCount.get() / (double)(lastTime - startTime) );
                    if (exitAfterAll) {
                        System.exit(0);
                    }
                }
                return task;
            }

            @Override
            public void release(TaskDecision taskDecision) {
                taskSpreader.release(taskDecision);
            }
        };
    }

    public void setExitAfterAll(boolean exitAfterAll) {
        this.exitAfterAll = exitAfterAll;
    }

    public void setTasksForStat(int tasksForStat) {
        this.tasksForStat = tasksForStat;
    }
}

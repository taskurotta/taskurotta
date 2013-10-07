package ru.taskurotta.recipes.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: greg
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {
    private final static Logger log = LoggerFactory.getLogger(LifetimeProfiler.class);

    private AtomicLong taskCount = new AtomicLong(0);
    private AtomicLong startTime = new AtomicLong(0);
    private AtomicLong lastTime = new AtomicLong(0);
    private int tasksForStat = 200;

    public LifetimeProfiler() {
    }

    public LifetimeProfiler(Class actorClass, Properties properties) {
        if (properties.containsKey("tasksForStat")) {
            tasksForStat = (Integer) properties.get("tasksForStat");
        }
    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {

                if (taskCount.get() == 0) {
                    long current = System.currentTimeMillis();
                    lastTime.set(current);
                    startTime.set(current);
                }

                Task task = taskSpreader.poll();

                long curTime = System.currentTimeMillis();
                if (null != task) {
                    long count = taskCount.incrementAndGet();
                    if (count % tasksForStat == 0) {
                        System.out.printf("       tasks: %6d; time: %6.3f s; rate: %8.3f tps\n", count, 0.001 * (curTime - lastTime.get()), 1000.0D * tasksForStat / (curTime - lastTime.get()));
                        lastTime.set(curTime);
                    }
                } else {
                    System.out.printf("TOTAL: tasks: %6d; time: %6.3f s; rate: %8.3f tps\n", taskCount.get(), 1.0 * (lastTime.get() - startTime.get()) / 1000.0, 1000.0 * taskCount.get() / (double) (lastTime.get() - startTime.get()));
                    taskCount.set(0);
                    if (StressTaskCreator.LATCH != null) {
                        StressTaskCreator.LATCH.countDown();
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

    public void setTasksForStat(int tasksForStat) {
        this.tasksForStat = tasksForStat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}

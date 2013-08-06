package ru.taskurotta.recipes.multiplier;

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
 * Created by void 12.07.13 18:29
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {
    private final static Logger log = LoggerFactory.getLogger(LifetimeProfiler.class);

    private TaskCreator taskCreator;
    private AtomicLong taskCount = new AtomicLong(0);
    private long startTime;
    private long lastTime;
    private boolean exitAfterAll;
    private int tasksForStat = 200;

    public LifetimeProfiler() {
    }

    public LifetimeProfiler(Class actorClass, Properties properties) {
        if (properties.containsKey("exitAfterAll")) {
            exitAfterAll = (Boolean)properties.get("exitAfterAll");
        }

        if (properties.containsKey("tasksForStat")) {
            tasksForStat = (Integer) properties.get("tasksForStat");
        }
    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {
                if (!TaskCreator.canWork.get()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) { //
                    }
                    return null;
                }

                if (taskCount.get() == 0) {
                    lastTime = startTime = System.currentTimeMillis();
                }

                Task task = taskSpreader.poll();

                long curTime = System.currentTimeMillis();
                if (null != task) {
                    long count = taskCount.incrementAndGet();
                    if (count % tasksForStat == 0) {
                        System.out.printf("       tasks: %6d; time: %6.3f s; rate: %8.3f tps\n", count, 0.001 * (curTime - lastTime), 1000.0D * tasksForStat / (curTime - lastTime));
                        lastTime = curTime;
                    }
                } else {
                    TaskCreator.Monitor.lock();

                    if (TaskCreator.canWork.get()) {
                        TaskCreator.canWork.set(false);
                        System.out.printf("TOTAL: tasks: %6d; time: %6.3f s; rate: %8.3f tps\n", taskCount.get(), 1.0 * (lastTime - startTime) / 1000.0, 1000.0 * taskCount.get() / (double)(lastTime - startTime) );
                        taskCount.set(0);
                        if (exitAfterAll) {
                            System.exit(0);
                        }
                    }

                    TaskCreator.Monitor.unlock();
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        taskCreator = (TaskCreator) applicationContext.getBean("taskCreator");
    }
}

package ru.taskurotta.test.stress;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.test.fullfeature.RuntimeExceptionHolder;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: greg
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {

    private final static Logger logger = LoggerFactory.getLogger(LifetimeProfiler.class);

    private final static String DROP_TASK_DECISION_EVERY_N_TASKS = "dropTaskDecisionEveryNTasks";
    private final static String BREAK_PROCESS_EVERY_N_TASKS = "breakProcessEveryNTasks";
    private final static String TASKS_FOR_STAT = "tasksForStat";


    public static AtomicLong taskCount = new AtomicLong(0);
    public static AtomicLong startedProcessCounter = new AtomicLong(0);
    public static AtomicLong startTime = new AtomicLong(0);
    public static AtomicLong lastTime = new AtomicLong(0);

    public static int tasksForStat = 1000;
    public static int dropTaskDecisionEveryNTasks = 0;
    public static int breakProcessEveryNTasks = 0;

    private boolean isFirstPoll = true;
    private AtomicInteger nullPoll = new AtomicInteger(0);

    public LifetimeProfiler() {
    }

    public LifetimeProfiler(Class actorClass, Properties properties) {

        String sys = null;

        if (properties.containsKey(TASKS_FOR_STAT)) {
            tasksForStat = Integer.valueOf((String) properties.getProperty(TASKS_FOR_STAT));
        }

        if (properties.containsKey(DROP_TASK_DECISION_EVERY_N_TASKS)) {
            dropTaskDecisionEveryNTasks = Integer.valueOf((String) properties.getProperty(DROP_TASK_DECISION_EVERY_N_TASKS));
        }

        if (properties.containsKey(BREAK_PROCESS_EVERY_N_TASKS)) {
            breakProcessEveryNTasks = Integer.valueOf((String) properties.getProperty(BREAK_PROCESS_EVERY_N_TASKS));
        }
    }


    public static AtomicBoolean isReady = new AtomicBoolean(false);

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {

        Set allHazelcastInstances = Hazelcast.getAllHazelcastInstances();

        final HazelcastInstance hazelcastInstance = allHazelcastInstances.size() == 1 ?
                (HazelcastInstance) Hazelcast.getAllHazelcastInstances().toArray()[0] : null;


        return new TaskSpreader() {
            @Override
            public Task poll() {

                Task task = taskSpreader.poll();
                if (task == null) {

                    int localNullPoll = nullPoll.incrementAndGet();

                    if ((localNullPoll + 1) % 1000 == 0) {
                        logger.error("Actors still receive empty answer [{}]", localNullPoll + 1);
                    }
                    return null;
                }

                long count = taskCount.incrementAndGet();

                // set exception for actor
                if (breakProcessEveryNTasks > 0 && count % breakProcessEveryNTasks == 0) {
                    RuntimeExceptionHolder.exceptionToThrow.set(new BrokenProcessException("Bad day...."));
                }

                if (isFirstPoll) {
                    long current = System.currentTimeMillis();
                    lastTime.set(current);
                    startTime.set(current);
                    isFirstPoll = false;
                }

                long curTime = System.currentTimeMillis();
                if (count % tasksForStat == 0) {
                    double time = 0.001 * (curTime - lastTime.get());
                    double rate = 1000.0D * tasksForStat / (curTime - lastTime.get());
                    double totalRate = 1000.0 * count / (double) (curTime - LifetimeProfiler.startTime.get());
                    lastTime.set(curTime);

                    logger.info(String.format("       tasks: %6d; time: %6.3f s; rate: %8.3f tps; " +
                                    "totalRate: %8.3f; freeMemory: %6d;\n", count, time, rate,
                            totalRate, Runtime.getRuntime().freeMemory() / 1024 / 1024));
                }

                return task;
            }

            @Override
            public void release(TaskDecision taskDecision) {

                if (dropTaskDecisionEveryNTasks > 0 && taskCount.get() % dropTaskDecisionEveryNTasks == 0) {
                    return;
                }

                taskSpreader.release(taskDecision);

                // clean thread local container
                RuntimeExceptionHolder.exceptionToThrow.set(null);
            }
        };
    }

    public void setTasksForStat(int tasksForStat) {
        LifetimeProfiler.tasksForStat = tasksForStat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}

package ru.taskurotta.test.stress;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Clock;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.SimpleProfiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.test.fullfeature.RuntimeExceptionHolder;
import ru.taskurotta.util.DaemonThread;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: greg
 */
public class LifetimeProfiler extends SimpleProfiler implements ApplicationContextAware {

    private final static Logger logger = LoggerFactory.getLogger(LifetimeProfiler.class);
    private static final Clock clock = Clock.defaultClock();


    private final static String DROP_TASK_DECISION_EVERY_N_TASKS = "dropTaskDecisionEveryNTasks";
    private final static String BREAK_PROCESS_EVERY_N_TASKS = "breakProcessEveryNTasks";
    private final static String TASKS_FOR_STAT = "tasksForStat";
    private final static String LOG_PROFILER_SECONDS = "logProfilerSeconds";

    private static ConcurrentMap<TaskTarget, Timer> executeTimers = new ConcurrentHashMap<TaskTarget, Timer>();
    public static AtomicBoolean isLogProfilerStarted = new AtomicBoolean(false);

    public static AtomicLong taskCount = new AtomicLong(0);
    public static AtomicLong startTime = new AtomicLong(0);
    public static AtomicLong lastTime = new AtomicLong(0);

    private int tasksForStat = 1000;
    private int dropTaskDecisionEveryNTasks = 0;
    private int breakProcessEveryNTasks = 0;
    private int logProfilerSeconds = 0;

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

        if (properties.containsKey(LOG_PROFILER_SECONDS)) {
            logProfilerSeconds = Integer.valueOf((String) properties.getProperty(LOG_PROFILER_SECONDS));
        }

        if (logProfilerSeconds > 0 && isLogProfilerStarted.compareAndSet(false, true)) {

            // start dump thread
            new DaemonThread("actor metrics logger", TimeUnit.SECONDS, logProfilerSeconds) {

                @Override
                public void daemonJob() {

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream out = new PrintStream(baos, true);

                    out.println("\n========= Actors execution metrics ==========");

                    if (executeTimers.isEmpty()) {
                        out.println("no actor metrics ...");
                    } else {

                        TreeMap<String, TaskTarget> orderedTaskTargets = new TreeMap<>();
                        for (TaskTarget taskTarget: executeTimers.keySet()) {
                            orderedTaskTargets.put(taskTarget.toString(), taskTarget);
                        }

                        for (String key : orderedTaskTargets.keySet()) {
                            TaskTarget target = orderedTaskTargets.get(key);
                            Timer timer = executeTimers.get(target);

                            out.printf("%1$s\t mean = %2$6.2f\t min = %3$6.2f max = %4$6.2f rate1 = %5$6.2f rate5" +
                                            " = %6$6.2f rate15 = %7$6.2f\n", getTargetName(target), timer.mean(),
                                    timer.min(), timer.max(), timer.oneMinuteRate(), timer.fiveMinuteRate(),
                                    timer.fifteenMinuteRate());
                        }
                        out.println();
                    }

                    logger.info(new String(baos.toByteArray()));
                }
            }.start();
        }

    }

    private static String getTargetName(TaskTarget taskTarget) {
        return taskTarget.getName() + '#' + taskTarget.getVersion() + '_' + taskTarget.getMethod();
    }

    private static Timer getExecuteTimer(TaskTarget taskTarget) {
        Timer timer = executeTimers.get(taskTarget);

        if (timer != null) {
            return timer;
        }

        // task type not used in metricsName. We hope there are no deciders and actors with same name and version
        // values
        MetricName metricName = new MetricName("ActorProfiler", "execute", taskTarget.getName() + "#" + taskTarget
                .getVersion(), taskTarget.getMethod());

        timer = Metrics.newTimer(metricName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

        // we can put and not worry about collisions because Metrics.newTimer should return same instance for same
        // metricName
        executeTimers.put(taskTarget, timer);

        return timer;
    }

    @Override
    public RuntimeProcessor decorate(final RuntimeProcessor runtimeProcessor) {

        if (logProfilerSeconds <= 0) {
            return runtimeProcessor;
        }

        return new RuntimeProcessor() {

            @Override
            public TaskDecision execute(Task task) {

                long startTime = System.currentTimeMillis();

                try {
                    return runtimeProcessor.execute(task);
                } finally {
                    getExecuteTimer(task.getTarget()).update(System.currentTimeMillis() - startTime, TimeUnit
                            .MILLISECONDS);
                }

            }

            @Override
            public Task[] execute(UUID processId, Runnable runnable) {
                throw new IllegalAccessError("Method not supported yet");
            }
        };

    }


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
        this.tasksForStat = tasksForStat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}

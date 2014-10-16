package ru.taskurotta.test.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: stukushin
 * Date: 16.10.2014
 * Time: 14:36
 */

public class LifeTimeProfiler implements Profiler {

    private static final Logger logger = LoggerFactory.getLogger(LifeTimeProfiler.class);

    private AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    private AtomicLong pollCounter = new AtomicLong();
    private ReentrantLock lock = new ReentrantLock();

    public LifeTimeProfiler(Class actorClass, Properties properties) {}

    @Override
    public RuntimeProcessor decorate(final RuntimeProcessor runtimeProcessor) {
        return new RuntimeProcessor() {
            @Override
            public TaskDecision execute(Task task) {
                return runtimeProcessor.execute(task);
            }

            @Override
            public Task[] execute(UUID processId, Runnable runnable) {
                return runtimeProcessor.execute(processId, runnable);
            }
        };
    }

    @Override
    public TaskSpreader decorate(final TaskSpreader taskSpreader) {
        return new TaskSpreader() {
            @Override
            public Task poll() {
                if (pollCounter.incrementAndGet() % 1000 == 0) {
                    if (lock.tryLock()) {
                        writeToLog(pollCounter.longValue());
                        lock.unlock();
                    }
                }

                return taskSpreader.poll();
            }

            @Override
            public void release(TaskDecision taskDecision) {
                taskSpreader.release(taskDecision);
            }
        };
    }

    @Override
    public void cycleStart() {}

    @Override
    public void cycleFinish() {}

    private void writeToLog(long poolCounter) {
        long workDuration = (System.currentTimeMillis() - startTime.longValue()) / 1000;
        if (workDuration == 0) {
            return;
        }

        double pollAvgSpeed = poolCounter / workDuration;
        logger.info("After [{}] tasks and [{}] seconds: pollAvgSpeed = [{}]/seconds", poolCounter, workDuration, pollAvgSpeed);
    }
}

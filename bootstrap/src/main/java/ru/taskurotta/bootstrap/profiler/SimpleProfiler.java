package ru.taskurotta.bootstrap.profiler;

import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.Heartbeat;

import java.util.UUID;

/**
 * User: stukushin
 * Date: 26.03.13
 * Time: 13:12
 */
public class SimpleProfiler implements Profiler {

    public SimpleProfiler() {
    }

    @Override
    public RuntimeProcessor decorate(final RuntimeProcessor runtimeProcessor) {
        return new RuntimeProcessor() {
            @Override
            public TaskDecision execute(Task task, Heartbeat heartbeat) {
                return runtimeProcessor.execute(task, heartbeat);
            }

            @Override
            public Task[] execute(UUID taskId, UUID processId, Heartbeat heartbeat, Runnable runnable) {
                return runtimeProcessor.execute(taskId, processId, heartbeat, runnable);
            }
        };
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
                taskSpreader.release(taskDecision);
            }

            @Override
            public void updateTimeout(UUID taskId, UUID processId, long timeout) {
                taskSpreader.updateTimeout(taskId, processId, timeout);
            }
        };
    }

    @Override
    public void cycleStart() {
    }

    @Override
    public void cycleFinish() {
    }
}

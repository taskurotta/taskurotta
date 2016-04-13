package ru.taskurotta.test.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.Heartbeat;

import java.util.Properties;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 26.03.13
 * Time: 13:12
 */
public class FlowArbiterProfiler implements Profiler {
	protected static final Logger log = LoggerFactory.getLogger(FlowArbiterProfiler.class);

	private FlowArbiter arbiter;

	public FlowArbiterProfiler(Class actorClass, Properties prop) {
		this(actorClass);
	}

    public FlowArbiterProfiler(Class actorClass) {
		arbiter = new FlowArbiterFactory().getInstance();
	}

    @Override
    public RuntimeProcessor decorate(final RuntimeProcessor runtimeProcessor) {
        return new RuntimeProcessor() {
            @Override
            public TaskDecision execute(Task task, Heartbeat heartbeat) {
				log.debug("before execute [{}]", task.getTarget());
				TaskDecision decision = runtimeProcessor.execute(task, heartbeat);
				arbiter.notify(task.getTarget().getMethod());
				return decision;
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
    public void cycleStart() {}

    @Override
    public void cycleFinish() {}
}

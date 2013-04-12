package ru.taskurotta.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

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
            public TaskDecision execute(Task task) {
				log.debug("before execute [{}]", task.getTarget());
				TaskDecision decision = runtimeProcessor.execute(task);
				arbiter.notify(task.getTarget().getMethod());
				return decision;
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
}

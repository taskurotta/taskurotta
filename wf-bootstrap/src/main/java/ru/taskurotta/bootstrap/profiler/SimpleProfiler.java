package ru.taskurotta.bootstrap.profiler;

import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

/**
 * User: stukushin
 * Date: 26.03.13
 * Time: 13:12
 */
public class SimpleProfiler implements Profiler {

	public SimpleProfiler(Class actorClass) {
	}

	@Override
	public RuntimeProcessor decorate(final RuntimeProcessor runtimeProcessor) {
		return new RuntimeProcessor() {
			@Override
			public TaskDecision execute(Task task) {
				return runtimeProcessor.execute(task);
			}

			@Override
			public Task[] execute(Runnable runnable) {
				return runtimeProcessor.execute(runnable);
			}
		};
	}

	@Override
	public TaskSpreader decorate(final TaskSpreader taskSpreader) {
		return new TaskSpreader() {
			@Override
			public Task pull() {
				return taskSpreader.pull();
			}

			@Override
			public void release(TaskDecision taskDecision) {
				taskSpreader.release(taskDecision);
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

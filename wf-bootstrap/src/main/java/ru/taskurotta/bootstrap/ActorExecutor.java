package ru.taskurotta.bootstrap;

import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.bootstrap.profiler.Profiler;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 12.02.13
 * Time: 19:29
 */
public class ActorExecutor implements Runnable {

    private Profiler profiler;
    private RetryPolicy retryPolicy;
    private RuntimeProcessor runtimeProcessor;
    private TaskSpreader taskSpreader;

    private boolean shutdown = false;

    public ActorExecutor(Profiler profiler, RetryPolicy retryPolicy, RuntimeProcessor runtimeProcessor, TaskSpreader taskSpreader) {
        this.profiler = profiler;
        this.retryPolicy = retryPolicy;
        this.runtimeProcessor = profiler.decorate(runtimeProcessor);
        this.taskSpreader = profiler.decorate(taskSpreader);
    }

    @Override
    public void run() {

        Date firstAttempt = new Date();
        int numberOfTries = 0;

        while (!shutdown) {

            profiler.cycleStart();

            try {
                // ToDo (stukushin): which exceptions will be catch for use retry politics?
                Task task = taskSpreader.poll();

                if (task == null) {
                    profiler.cycleFinish();

                    numberOfTries++;
                    if (numberOfTries > 2) {
                        long nextRetryDelaySeconds = retryPolicy.nextRetryDelaySeconds(firstAttempt, new Date(), numberOfTries);
                        try {
                            TimeUnit.SECONDS.sleep(nextRetryDelaySeconds);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    continue;
                }

                // TODO: catch all exceptions and send it to server
                TaskDecision taskDecision = runtimeProcessor.execute(task);

                // ToDo (stukushin): which exceptions will be catch for use retry politics?
                taskSpreader.release(taskDecision);

            } finally {
                profiler.cycleFinish();
            }

        }
    }

	public void stop() {
		shutdown = true;
	}
}


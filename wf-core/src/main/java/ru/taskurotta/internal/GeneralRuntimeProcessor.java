package ru.taskurotta.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.UndefinedActorException;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.util.Map;
import java.util.UUID;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 4:35 PM
 */
public class GeneralRuntimeProcessor implements RuntimeProcessor {

    protected final static Logger log = LoggerFactory.getLogger(GeneralRuntimeProcessor.class);

    private Map<TaskTarget, GeneralRuntimeProvider.TargetReference> taskTargetsMap;

    public GeneralRuntimeProcessor(Map<TaskTarget, GeneralRuntimeProvider.TargetReference> taskTargetsMap) {

        this.taskTargetsMap = taskTargetsMap;
    }


    @Override
    public TaskDecision execute(Task task) {

        RuntimeContext.start(task.getProcessId());

        TaskDecision taskDecision = null;
        TaskTarget key = task.getTarget();

        GeneralRuntimeProvider.TargetReference targetReference = taskTargetsMap.get(key);
        try {

            if (null == targetReference) {
                log.error("Cannot execute task[{}]: actor undefined. Current task targets are[{}]", task, taskTargetsMap.keySet());
                taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(), new UndefinedActorException(key), RuntimeContext.getCurrent().getTasks());
            } else {

                Object value = targetReference.invoke(task.getArgs());

                Task[] tasks = RuntimeContext.getCurrent().getTasks();
                taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(), value, tasks);
            }
        } catch(Throwable e) {
            log.error("Unexpected error processing task ["+task+"]", e);

            assert targetReference != null;
            RetryPolicy retryPolicy = targetReference.getRetryPolicy();

            long restartTime = TaskDecision.NO_RESTART;
            if (retryPolicy != null && retryPolicy.isRetryable(e)) {
                long nextRetryDelaySeconds = 0;
                long recordedFailure = System.currentTimeMillis();
                if (task.getNumberOfAttempts() > 2) {
                    nextRetryDelaySeconds = retryPolicy.nextRetryDelaySeconds(task.getStartTime(), recordedFailure, task.getNumberOfAttempts());
                }

                restartTime = recordedFailure + nextRetryDelaySeconds * 1000;
            }

            taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(), e, RuntimeContext.getCurrent().getTasks(), restartTime);

        } finally {
            RuntimeContext.finish();
        }

        return taskDecision;
    }

    @Override
    public Task[] execute(UUID processId, Runnable runnable) {

        RuntimeContext.start(processId);

        Task[] tasks;

        try {
            runnable.run();
            tasks = RuntimeContext.getCurrent().getTasks();

        } finally {
            RuntimeContext.finish();
        }

        return tasks;
    }
}

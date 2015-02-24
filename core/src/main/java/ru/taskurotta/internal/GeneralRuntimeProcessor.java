package ru.taskurotta.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.Environment;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.UndefinedActorException;
import ru.taskurotta.exception.test.TestException;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.policy.PolicyConstants;
import ru.taskurotta.policy.retry.RetryPolicy;

import java.lang.reflect.InvocationTargetException;
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

            if (targetReference == null) {
                log.error("Cannot execute task[{}]: actor undefined. Current task targets are[{}]", task, taskTargetsMap.keySet());
                taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(), new UndefinedActorException(key), RuntimeContext.getCurrent().getTasks());
            } else {

                long startTime = System.currentTimeMillis();

                Object value = targetReference.invoke(task.getArgs());

                long executionTime = System.currentTimeMillis() - startTime;

                Task[] tasks = RuntimeContext.getCurrent().getTasks();

                taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(), value, tasks, executionTime);
            }
        } catch (Throwable e) {

            log.error("Unexpected error processing task [" + task + "]", e);

            assert targetReference != null;
            RetryPolicy retryPolicy = targetReference.getRetryPolicy();

            long restartTime = TaskDecision.NO_RESTART;
            if (retryPolicy != null && retryPolicy.isRetryable(e)) {
                long recordedFailure = System.currentTimeMillis();
                long nextRetryDelaySeconds = retryPolicy.nextRetryDelaySeconds(task.getStartTime(), recordedFailure, task.getErrorAttempts());

                if (nextRetryDelaySeconds != PolicyConstants.NONE) {
                    restartTime = recordedFailure + nextRetryDelaySeconds * 1000;
                }
            }

            if (Environment.getInstance().getType() == Environment.Type.TEST) {
                if (e.getCause() instanceof TestException){
                    throw new RuntimeException(e);
                }
            }

            if (e instanceof InvocationTargetException && e.getCause() != null) {
                e = e.getCause();//to send real error to the server
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

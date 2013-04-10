package ru.taskurotta.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.ActorExecutionException;
import ru.taskurotta.exception.ActorRuntimeException;
import ru.taskurotta.exception.Retriable;
import ru.taskurotta.exception.UndefinedActorException;
import ru.taskurotta.internal.core.TaskDecisionImpl;

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
                taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(), new UndefinedActorException(key), RuntimeContext.getCurrent().getTasks());
            } else {

                Object value = targetReference.invoke(task.getArgs());

                Task[] tasks = RuntimeContext.getCurrent().getTasks();
                taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(), value, tasks);
            }
        } catch (IllegalAccessException e) {
            log.debug("Can't call method [" + targetReference + "]", e);
            taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(),
                    new ActorRuntimeException("Can't call method [" + targetReference + "]", e), RuntimeContext.getCurrent().getTasks());
        } catch (InvocationTargetException e) {
            log.debug("Can't call method [{}], exception[{}]", targetReference, e);
            taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(),
                    prepareException(e.getCause()), RuntimeContext.getCurrent().getTasks());
        } catch(Throwable e) {
            log.error("Unexpected error processing task ["+task+"]", e);
            taskDecision = new TaskDecisionImpl(task.getId(), task.getProcessId(),
                    prepareException(e), RuntimeContext.getCurrent().getTasks());
        } finally {
            RuntimeContext.finish();
        }

        return taskDecision;
    }


    private ActorExecutionException prepareException(Throwable e) {
        ActorExecutionException result = new ActorExecutionException(e);
        if(e instanceof Retriable) {//TODO: require some updates with retry policy
            result.setShouldBeRestarted(((Retriable)e).isShouldBeRestarted());
            result.setRestartTime(((Retriable)e).getRestartTime());
        }
        return result;
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

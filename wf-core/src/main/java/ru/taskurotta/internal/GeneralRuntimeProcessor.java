package ru.taskurotta.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.ActorRuntimeException;
import ru.taskurotta.exception.TargetException;
import ru.taskurotta.exception.UndefinedActorException;
import ru.taskurotta.internal.core.TaskDecisionImpl;

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

            if (null == targetReference) {
                throw new UndefinedActorException(key);
            } else {

                Object value = targetReference.invoke(task.getArgs());

                Task[] tasks = RuntimeContext.getCurrent().getTasks();
                taskDecision = new TaskDecisionImpl(task.getId(), value, tasks);
            }
        } catch (IllegalAccessException e) {
            log.error("Can't call method [" + targetReference + "]", e);
            throw new ActorRuntimeException("Can't call method [" + targetReference + "]", e);
        } catch (InvocationTargetException e) {
            log.error("Can't call method [" + targetReference + "]", e);
            throw new TargetException(targetReference.toString(), e);
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

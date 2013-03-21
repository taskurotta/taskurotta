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
import java.util.List;
import java.util.Map;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 4:35 PM
 */
public class GeneralRuntimeProcessor implements RuntimeProcessor {

    protected final static Logger log = LoggerFactory.getLogger(GeneralRuntimeProcessor.class);

    private Map<TaskTarget, GeneralRuntimeProvider.TargetReference> taskTargetsMap;

    protected ThreadLocal<List<Task>> tlTaskList;


    public GeneralRuntimeProcessor(Map<TaskTarget, GeneralRuntimeProvider.TargetReference> taskTargetsMap, ThreadLocal<List<Task>> tlTaskList) {

        this.taskTargetsMap = taskTargetsMap;
        this.tlTaskList = tlTaskList;
    }


    @Override
    public TaskDecision execute(Task task) {
        TaskDecision taskDecision = null;
        TaskTarget key = task.getTarget();

        GeneralRuntimeProvider.TargetReference targetReference = taskTargetsMap.get(key);
        try {

            if (null == targetReference) {
                throw new UndefinedActorException(key);
            } else {

                // Better safe than sorry :)
                tlTaskList.remove();

                Object value = targetReference.invoke(task.getArgs());

                List<Task> taskList = tlTaskList.get();
                Task[] tasks;
                if (taskList == null) {
                    tasks = null;
                } else {
                    tasks = new Task[taskList.size()];
                    taskList.toArray(tasks);
                }
                taskDecision = new TaskDecisionImpl(task.getId(), value, tasks);
            }
        } catch (IllegalAccessException e) {
            log.error("Can't call method [" + targetReference + "]", e);
            throw new ActorRuntimeException("Can't call method [" + targetReference + "]", e);
        } catch (InvocationTargetException e) {
            log.error("Can't call method [" + targetReference + "]", e);
            throw new TargetException(targetReference.toString(), e);
        } finally {
            tlTaskList.remove();
        }

        return taskDecision;
    }


    @Override
    public List<Task> execute(Runnable runnable) {

        // Better safe than sorry :)
        tlTaskList.remove();

        List<Task> taskList;

        try {
            runnable.run();
            taskList = tlTaskList.get();
        } finally {
            tlTaskList.remove();
        }

        return taskList;
    }
}

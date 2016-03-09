package ru.taskurotta.transport.utils;

import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

/**
 * Date: 06.02.14 12:24
 */
public class TransportUtils {

    public static final String REST_SERVICE_PREFIX = "/rest/";

    public static String getTaskList(TaskContainer taskContainer) {
        String result = null;

        if (taskContainer != null) {
            TaskOptionsContainer taskOptionsContainer = taskContainer.getOptions();
            if (taskOptionsContainer != null) {
                TaskConfigContainer taskConfigContainer = taskOptionsContainer.getTaskConfigContainer();
                if (taskConfigContainer != null) {
                    result = taskConfigContainer.getTaskList();
                }
            }
        }

        return result;
    }

    /**
     * Java
     */
    public static String getCustomId(TaskContainer taskContainer) {
        TaskOptionsContainer taskOptions = taskContainer.getOptions();
        if (taskOptions == null) {
            return null;
        }
        TaskConfigContainer taskConfigContainer = taskOptions.getTaskConfigContainer();
        if (taskConfigContainer == null) {
            return null;
        }
        return taskConfigContainer.getCustomId();
    }


    public static String createQueueName(String actorId, String taskList, String queueNamePrefix) {
        return createQueueName((queueNamePrefix != null ? queueNamePrefix + actorId : actorId), taskList);
    }

    public static String createQueueName(String actorId, String taskList) {
        return (taskList == null) ? actorId : actorId + ActorUtils.SEPARATOR + taskList;
    }

    public static String getRestPath(String endpoint, String path) {
        if (endpoint != null && path != null) {
            return endpoint.replaceAll("/*$", "") + REST_SERVICE_PREFIX + path.replaceAll("^/*", "");
        }
        return null;
    }

    public static boolean hasFatalError(DecisionContainer decision) {
        return decision != null && decision.getErrorContainer() != null && decision.getErrorContainer().isFatalError();
    }

    public static String trimToLength(String target, int length) {
        if (target == null || target.length() <= length) {
            return target;
        } else {
            return target.substring(0, length);
        }
    }

    public static ActorDefinition getActorDefinition(TaskContainer taskContainer) {
        if (taskContainer != null && taskContainer.getActorId() != null) {
            String taskList = getTaskList(taskContainer);
            String actorId = taskContainer.getActorId();
            int idx = actorId.lastIndexOf(ActorUtils.SEPARATOR);
            if (idx > 0) {
                return ActorDefinition.valueOf(actorId.substring(0, idx), actorId.substring(idx + 1), taskList);
            } else {
                return ActorDefinition.valueOf(actorId, "", taskList);
            }
        }
        return null;

    }

    public static String getFullActorName(TaskContainer taskContainer) {
        if (taskContainer == null) {
            return null;
        }

        String result = taskContainer.getActorId();
        String taskList = getTaskList(taskContainer);

        if (taskList != null) {
            return result + ActorUtils.SEPARATOR + taskList;
        }

        return result;
    }

}

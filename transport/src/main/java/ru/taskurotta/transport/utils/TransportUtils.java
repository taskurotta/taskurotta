package ru.taskurotta.transport.utils;

import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

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

    public static String createQueueName(String actorId, String taskList, String queueNamePrefix) {
        return createQueueName((queueNamePrefix!=null? queueNamePrefix+actorId: actorId), taskList);
    }

    public static String createQueueName(String actorId, String taskList) {
        return (taskList == null) ? actorId : actorId + "#" + taskList;
    }

    public static String getRestPath(String endpoint, String path) {
        if (endpoint != null && path != null) {
            return endpoint.replaceAll("/*$", "") + REST_SERVICE_PREFIX + path.replaceAll("^/*", "");
        }
        return null;
    }

    public static boolean hasFatalError(DecisionContainer decision) {
        return decision!=null && decision.getErrorContainer()!=null && decision.getErrorContainer().isFatalError();
    }

    public static String trimToLength(String target, int length) {
        if (target == null || target.length()<=length) {
            return target;
        } else {
            return target.substring(0, length);
        }
    }

}

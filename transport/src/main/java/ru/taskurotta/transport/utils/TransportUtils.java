package ru.taskurotta.transport.utils;

import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

/**
 * Date: 06.02.14 12:24
 */
public class TransportUtils {

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

}

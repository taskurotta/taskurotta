package ru.taskurotta.dropwizard.resources.console.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.taskurotta.dropwizard.resources.console.schedule.model.ArgVO;
import ru.taskurotta.dropwizard.resources.console.schedule.model.TaskCommand;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.schedule.JobConstants;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.UUID;

/**
 * Created on 21.04.2015.
 */
public class TaskContainerUtils {

    private static final Logger logger = LoggerFactory.getLogger(TaskContainerUtils.class);

    private static ObjectMapper mapper = new ObjectMapper();

    public static TaskContainer createTask(TaskCommand command, long startTime) {
        validateCommand(command);
        UUID guid = UUID.randomUUID();
        TaskType type = command.getTaskType() != null? command.getTaskType(): TaskType.DECIDER_START;

        String actorId = command.getActorId().replaceAll("\\s", "");
        TaskOptionsContainer toc = null;
        if (StringUtils.hasText(command.getTaskList())) {
            TaskConfigContainer tcc = new TaskConfigContainer();
            tcc.setTaskList(command.getTaskList());
            toc = new TaskOptionsContainer(null, tcc, null);
        }

        return new TaskContainer(guid, guid, null, command.getMethod(), actorId,
                type, startTime, JobConstants.DEFAULT_NUMBER_OF_ATTEMPTS,
                getTaskArguments(guid, command.getArgs()), toc, false, null);
    }

    public static void validateCommand(TaskCommand taskCommand) {
        if (taskCommand == null) {
            throw new IllegalArgumentException("taskCommand is null");
        } else if (!StringUtils.hasText(taskCommand.getActorId())) {
            throw new IllegalArgumentException("taskCommand actorId is empty!");
        } else if (!StringUtils.hasText(taskCommand.getMethod())) {
            throw new IllegalArgumentException("taskCommand method is empty!");
        }
    }

    public static ArgContainer[] getTaskArguments(UUID taskId, ArgVO[] args) {
        ArgContainer[] result = null;
        if (args != null && args.length>0) {
            int size = args.length;
            result = new ArgContainer[size];
            for(int i = 0; i<size; i++) {
                ArgVO arg = args[i];
                ArgContainer ac = new ArgContainer();
                ac.setValueType(ArgContainer.ValueType.PLAIN);
                ac.setPromise(false);
                ac.setReady(true);
                ac.setTaskId(taskId);

                populateArgContainerValue(ac, arg.getType(), arg.getValue());

                result[i] = ac;
                logger.debug("Resulting task [{}] argument is[{}]", taskId, ac);
            }
        }

        return result;
    }

    public static void populateArgContainerValue(ArgContainer ac, String valueType, String value) {
        Class valueClass = null;
        try {
            if (StringUtils.hasText(valueType)) {
                valueClass = Thread.currentThread().getContextClassLoader().loadClass("java.lang." + capFirst(valueType.trim().toLowerCase()));
            }

            if (valueClass != null) {
                ac.setDataType(valueClass.getName());
                Object valueAsObject = valueClass.getConstructor(String.class).newInstance(value);
                ac.setJSONValue(mapper.writeValueAsString(valueAsObject));
            } else {
                ac.setDataType(null);
                ac.setJSONValue(null);
            }

        } catch(Exception e) {
            String message = "Cannot populate argument["+ac+"] value["+value+"] with type ["+valueType+"]";
            logger.error(message, e);
            throw new IllegalArgumentException(message);
        }

    }

    public static String capFirst(String target) {
        if (target!=null && target.length()>0) {
            return target.substring(0, 1).toUpperCase() + target.substring(1);
        } else {
            return target;
        }
    }

}

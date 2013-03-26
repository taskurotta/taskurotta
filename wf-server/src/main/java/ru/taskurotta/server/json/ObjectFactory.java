package ru.taskurotta.server.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.server.transport.ArgContainer;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.ErrorContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.server.transport.TaskOptions;

import java.io.IOException;
import java.util.UUID;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 4:20 PM
 */
public class ObjectFactory {

    private ObjectMapper mapper;

    public ObjectFactory() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public Object parseArg(ArgContainer argContainer) {

        if (argContainer == null) {
            return null;
        }

        Object value = null;

        String json = argContainer.getJSONValue();

        if (json != null) {

            String className = argContainer.getClassName();
            Class loadedClass = null;

            try {
                loadedClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                // TODO: create new RuntimeException type
                throw new RuntimeException("Can not instantiate Object from json. Specified class not found: " + className, e);
            }

            try {
                value = mapper.readValue(argContainer.getJSONValue(), loadedClass);
            } catch (IOException e) {
                // TODO: create new RuntimeException type
                throw new RuntimeException("Can not instantiate Object from json. JSON value: " + argContainer.getJSONValue(), e);
            }

        }

        if (argContainer.isPromise()) {

            Promise promise = Promise.createInstance(argContainer.getTaskId());

            if (argContainer.isReady()) {
                promise.set(value);
            }

            return promise;
        }

        return value;
    }


    public ArgContainer dumpArg(Object arg) {

        if (arg == null) {
            return null;
        }

        String className = null;
        boolean isPromise = false;
        UUID taskId = null;
        boolean isReady = false;
        String jsonValue = null;

        if (arg instanceof Promise) {
            isPromise = true;
            taskId = ((Promise) arg).getId();
            isReady = ((Promise) arg).isReady();

            if (isReady) {
                arg = ((Promise) arg).get();
            } else {
                arg = null;
            }
        }

        if(arg!=null) {

            className = arg.getClass().getName();

            try {
                jsonValue = mapper.writeValueAsString(arg);
            } catch (JsonProcessingException e) {
                // TODO: create new RuntimeException type
                throw new RuntimeException("Can not create json String from Object: " + arg, e);
            }
        }

        return new ArgContainer(className, isPromise, taskId, isReady, jsonValue);
    }


    public Task parseTask(TaskContainer taskContainer) {
        UUID taskId = taskContainer.getTaskId();
        TaskTarget taskTarget = taskContainer.getTarget();
        Object[] args = null;

        ArgContainer[] argContainers = taskContainer.getArgs();

        if (argContainers != null) {
            args = new Object[argContainers.length];

            int i = 0;
            for (ArgContainer argContainer : argContainers) {
                args[i++] = parseArg(argContainer);
            }
        }

        return new TaskImpl(taskId, taskTarget, args);
    }


    public TaskContainer dumpTask(Task task) {
        UUID taskId = task.getId();
        TaskTarget target = task.getTarget();
        ArgContainer[] argContainers = null;

        Object[] args = task.getArgs();

        if (args != null) {
            argContainers = new ArgContainer[args.length];

            int i = 0;
            for (Object arg : args) {
                argContainers[i++] = dumpArg(arg);
            }
        }

		TaskOptions options = new TaskOptions(task.getSchedulingOptions().getArgTypes());

        return new TaskContainer(taskId, target, argContainers, options);
    }


    public TaskDecision parseResult(DecisionContainer decisionContainer) {

        // TODO: TaskDecision can be error.

        UUID taskId = decisionContainer.getTaskId();
        Object value = null;
        Task[] tasks = null;

        ArgContainer argContainer = decisionContainer.getValue();
        value = parseArg(argContainer);

        TaskContainer[] taskContainers = decisionContainer.getTasks();

        if (taskContainers != null) {
            tasks = new Task[taskContainers.length];

            int i = 0;
            for (TaskContainer taskContainer : taskContainers) {
                tasks[i++] = parseTask(taskContainer);
            }
        }

        return new TaskDecisionImpl(taskId, value, tasks);
    }


    public DecisionContainer dumpResult(TaskDecision taskDecision) {
        UUID taskId = taskDecision.getId();
        ArgContainer value = dumpArg(taskDecision.getValue());
        boolean isError = false;
        ErrorContainer errorContainer = null;
        TaskContainer[] taskContainers = null;

        Task[] tasks = taskDecision.getTasks();

        if (tasks != null) {
            taskContainers = new TaskContainer[tasks.length];

            int i = 0;
            for (Task task : tasks) {
                taskContainers[i++] = dumpTask(task);
            }
        }

        return new DecisionContainer(taskId, value, isError, errorContainer, taskContainers);
    }
}

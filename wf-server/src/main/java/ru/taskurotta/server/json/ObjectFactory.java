package ru.taskurotta.server.json;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.ActorExecutionException;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 4:20 PM
 */
public class ObjectFactory {

    private static final Logger logger = LoggerFactory.getLogger(ObjectFactory.class);

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
        boolean isArray = argContainer.isArray();

        if (json != null) {

            String className = argContainer.getClassName();
            Class loadedClass = null;

            try {
                value = isArray? getArrayValue(json, className): getSimpleValue(json, className);
            } catch (Exception e) {
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

    private Object getSimpleValue(String json, String valueClass) throws ClassNotFoundException, JsonParseException, JsonMappingException, IOException {
        Class loadedClass = Thread.currentThread().getContextClassLoader().loadClass(valueClass);
        return mapper.readValue(json, loadedClass);
    }

    private Object getArrayValue(String json, String arrayItemClass) throws Exception {
        JsonNode node = mapper.readTree(json);
        Class clazz = Class.forName(arrayItemClass);
        Object array = Array.newInstance(clazz, node.size());
        Iterator<JsonNode> iterator = node.elements();
        int i = 0;
        while(iterator.hasNext()) {
            JsonNode item = iterator.next();
            Array.set(array, i++, clazz.getConstructor(String.class).newInstance(item.textValue()));
        }
        return array;
    }

    public ArgContainer dumpArg(Object arg) {

        if (arg == null) {
            return null;
        }
        boolean isArray =arg.getClass().isArray();
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

        if (arg != null) {

            if(isArray) {
                className = arg.getClass().getComponentType().getName();
            } else {
                className = arg.getClass().getName();
            }

            try {
                jsonValue = isArray? writeAsJsonArray(arg): mapper.writeValueAsString(arg);
                logger.debug("Arg container JsonValue getted is [{}]", jsonValue);
            } catch (JsonProcessingException e) {
                // TODO: create new RuntimeException type
                throw new RuntimeException("Can not create json String from Object: " + arg, e);
            }
        }

        ArgContainer result =  new ArgContainer(className, isPromise, taskId, isReady, jsonValue, isArray);
        logger.debug("Created new ArgContainer[{}]", result);
        return result;
    }


    public Task parseTask(TaskContainer taskContainer) {

        if (taskContainer == null) {
            return null;
        }
        UUID processId = taskContainer.getProcessId();
        UUID taskId = taskContainer.getTaskId();
        ActorDefinition actorDef = ActorUtils.getActorDefinition(taskContainer.getActorId());
        TaskTarget taskTarget = new TaskTargetImpl(taskContainer.getType(), actorDef.getName(), actorDef.getVersion(), taskContainer.getMethod());
        Object[] args = null;

        ArgContainer[] argContainers = taskContainer.getArgs();

        if (argContainers != null) {
            args = new Object[argContainers.length];

            int i = 0;
            for (ArgContainer argContainer : argContainers) {
                args[i++] = parseArg(argContainer);
            }
        }

        return new TaskImpl(taskId, processId, taskTarget, taskContainer.getStartTime(), taskContainer.getNumberOfAttempts(), args, null);
    }


    public TaskContainer dumpTask(Task task) {
        UUID taskId = task.getId();
        UUID processId = task.getProcessId();
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

        TaskOptionsContainer taskOptionsContainer = dumpTaskOptions(task.getTaskOptions());

        return new TaskContainer(taskId, processId, target.getMethod(), ActorUtils.getActorId(target),
                target.getType(), task.getStartTime(), task.getNumberOfAttempts(), argContainers, taskOptionsContainer);
    }


    public TaskOptionsContainer dumpTaskOptions(TaskOptions taskOptions) {

        if (taskOptions == null) {
            return null;
        }

        return new TaskOptionsContainer(taskOptions.getArgTypes());
    }


    public TaskDecision parseResult(DecisionContainer decisionContainer) {

        UUID taskId = decisionContainer.getTaskId();
        UUID processId = decisionContainer.getProcessId();

        TaskContainer[] taskContainers = decisionContainer.getTasks();
        Task[] tasks = null;
        if (taskContainers != null) {
            tasks = new Task[taskContainers.length];

            int i = 0;
            for (TaskContainer taskContainer : taskContainers) {
                tasks[i++] = parseTask(taskContainer);
            }
        }

        if(decisionContainer.containsError()) {
            ErrorContainer error = decisionContainer.getErrorContainer();
            Throwable errValue = parseError(error);
            return new TaskDecisionImpl(taskId, processId, errValue, tasks);
        } else {
            ArgContainer argContainer = decisionContainer.getValue();
            Object value = parseArg(argContainer);
            return new TaskDecisionImpl(taskId, processId, value, tasks);
        }

    }


    public DecisionContainer dumpResult(TaskDecision taskDecision) {
        UUID taskId = taskDecision.getId();
        UUID processId = taskDecision.getProcessId();

        ArgContainer value = dumpArg(taskDecision.getValue());
        ErrorContainer errorContainer = dumpError(taskDecision.getException());
        TaskContainer[] taskContainers = null;

        Task[] tasks = taskDecision.getTasks();

        if (tasks != null) {
            taskContainers = new TaskContainer[tasks.length];

            int i = 0;
            for (Task task : tasks) {
                taskContainers[i++] = dumpTask(task);
            }
        }

        return new DecisionContainer(taskId, processId, value, errorContainer, taskContainers);
    }

    public ErrorContainer dumpError(Throwable e) {
        if(e == null) {
            return null;
        }
        ErrorContainer result = new ErrorContainer();
        result.setClassName(e.getClass().getName());
        result.setMessage(e.getMessage());
        result.setStackTrace(ErrorContainer.convert(e.getStackTrace()));

        if(ActorExecutionException.class.isAssignableFrom(e.getClass())) {
            ActorExecutionException aee = (ActorExecutionException) e;
            result.setRestartTime(aee.getRestartTime());
            result.setShouldBeRestarted(aee.isShouldBeRestarted());
        }

        return result;

    }

    private Throwable parseError(ErrorContainer error) {
        if(error == null) {
            return null;
        }
        Throwable result = new Throwable(error.getMessage());

        if(isExistingClass(error.getClassName())) {
            result = new ActorExecutionException(error.getMessage(), error.isShouldBeRestarted(), error.getRestartTime());
        } else {
            result = new Throwable(error.getMessage());
        }

        result.setStackTrace(ErrorContainer.convert(error.getStackTrace()));

        return result;
    }

    private static boolean isExistingClass(String className) {
        boolean result = false;
        try {
            Class.forName(className);
            result = true;
        } catch(ClassNotFoundException e) {
            logger.debug("Cannot instantiate Throwable of class[{}], error is[{}]", className, e);
        }
        return result;
    }

    private String writeAsJsonArray(Object array) throws ArrayIndexOutOfBoundsException, JsonProcessingException, IllegalArgumentException {
        ArrayNode arrayNode = mapper.createArrayNode();
        if(array!=null) {

            int size = Array.getLength(array);
            for(int i = 0; i<size; i++) {
                String itemValue =mapper.writeValueAsString(Array.get(array, i));
                arrayNode.add(itemValue);
            }
        }

        return arrayNode.toString();
    }
}

package ru.taskurotta.server.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.util.ActorDefinition;
import ru.taskurotta.util.ActorUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.UUID;

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
        if (json != null) {

            String className = argContainer.getClassName();
            try {
                value = argContainer.isArray()? getArrayValue(json, className): getSimpleValue(json, className);
            } catch (Exception e) {
                // TODO: create new RuntimeException type
                throw new RuntimeException("Can not instantiate Object from json type["+argContainer.getType()+"]. JSON value: " + argContainer.getJSONValue(), e);
            }
        } else {
            ArgContainer[] compositeValue = argContainer.getCompositeValue();
            if (null != compositeValue) {
                try {
                    value = Array.newInstance(Class.forName(argContainer.getClassName()), compositeValue.length);
                    for (int i = 0; i < compositeValue.length; i++) {
                        Array.set(value, i, parseArg(compositeValue[i]));
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Can not instantiate ObjectArray", e);
                }
            }
        }


        if (argContainer.isPromise()) {
            Promise promise = Promise.createInstance(argContainer.getTaskId());
            if (argContainer.isReady()) {
                //noinspection unchecked
                promise.set(value);
            }
            return promise;
        }

        return value;
    }

    private Object getSimpleValue(String json, String valueClass) throws ClassNotFoundException, IOException {
        Class loadedClass = Thread.currentThread().getContextClassLoader().loadClass(valueClass);
        return mapper.readValue(json, loadedClass);
    }

    private Object getArrayValue(String json, String arrayItemClassName) throws Exception {
        JsonNode node = mapper.readTree(json);
        ObjectCodec objectCodec = mapper.treeAsTokens(node).getCodec();

        Object array = ArrayFactory.newInstance(arrayItemClassName, node.size());
        Class<?> componentType = array.getClass().getComponentType();

        for (int i = 0; i < node.size(); i++) {
            Array.set(array, i, objectCodec.treeToValue(node.get(i), componentType));
        }
        return array;
    }

    public ArgContainer dumpArg(Object arg) {

        ArgContainer.ValueType type = ArgContainer.ValueType.PLAIN;
        UUID taskId = null;
        boolean isReady = true;

        if (arg instanceof Promise) {
            type = ArgContainer.ValueType.PROMISE;
            taskId = ((Promise) arg).getId();
            isReady = ((Promise) arg).isReady();
            arg = isReady? ((Promise) arg).get() : null;
        }

        ArgContainer result;
        String className = null;
        String jsonValue = null;
        if (arg != null) {
            try {
                if (arg.getClass().isArray()) {
                    className = arg.getClass().getComponentType().getName();
                    if (arg.getClass().getComponentType().isAssignableFrom(Promise.class)) {
                        type = ArgContainer.ValueType.OBJECT_ARRAY;
                        ArgContainer[] compositeValue = writeAsObjectArray(arg) ;
                        result = new ArgContainer(className, type, taskId, isReady, compositeValue);
                    } else {
                        type = ArgContainer.ValueType.ARRAY;
                        jsonValue = writeAsArray(arg) ;
                        result = new ArgContainer(className, type, taskId, isReady, jsonValue);
                    }
                } else {
                    className = arg.getClass().getName();
                    jsonValue = mapper.writeValueAsString(arg);
                    result = new ArgContainer(className, type, taskId, isReady, jsonValue);
                }

            } catch (JsonProcessingException e) {
                // TODO: create new RuntimeException type
                throw new RuntimeException("Can not create json String from Object: " + arg, e);
            }
        } else {
            result = new ArgContainer(className, type, taskId, isReady, jsonValue);
        }

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

        return new DecisionContainer(taskId, processId, value, errorContainer, taskDecision.getRestartTime(), taskContainers);
    }

    public ErrorContainer dumpError(Throwable e) {
        if (e == null) {
            return null;
        }

        return new ErrorContainer(e);
    }

    public String writeAsString(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    private String writeAsArray(Object array) throws JsonProcessingException {
        ArrayNode arrayNode = mapper.createArrayNode();
        if (array != null) {

            int size = Array.getLength(array);
            for (int i = 0; i<size; i++) {
                String itemValue = mapper.writeValueAsString(Array.get(array, i));
                arrayNode.add(itemValue);
            }
        }

        return arrayNode.toString();
    }

    private ArgContainer[] writeAsObjectArray(Object array) throws JsonProcessingException {
        ArgContainer[] result = null;

        if (array != null) {
            int size = Array.getLength(array);
            result = new ArgContainer[size];
            for (int i = 0; i<size; i++) {
                result[i] = dumpArg(Array.get(array, i));
            }
        }

        return result;
    }
}

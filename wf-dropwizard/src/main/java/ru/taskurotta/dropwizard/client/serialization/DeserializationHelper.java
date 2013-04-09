package ru.taskurotta.dropwizard.client.serialization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.ArgType;

import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.StackTraceElementContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;

import com.fasterxml.jackson.databind.JsonNode;

public class DeserializationHelper implements Constants {

    private static final Logger logger = LoggerFactory.getLogger(DeserializationHelper.class);

    public static String getStringValue(JsonNode node, String defVal) {
        String result = defVal;
        if(node!=null && !node.isNull()) {
            if(node.isTextual()) {
                result = node.textValue();
            } else if(node.isNumber()) {
                result = String.valueOf(node.longValue());
            } else if(node.isBoolean()) {
                result = String.valueOf(node.booleanValue());
            }
        }
        return result;
    }

    public static int getIntegerValue(JsonNode node, int defVal) {
        int result = defVal;
        if(node!=null && !node.isNull()) {
            if(node.isTextual()) {
                result = Integer.valueOf(node.textValue());
            } else if(node.isNumber()) {
                result = node.intValue();
            }
        }
        return result;
    }

    public static long getLongValue(JsonNode node, long defVal) {
        long result = defVal;
        if(node!=null && !node.isNull()) {
            if(node.isTextual()) {
                result = Long.valueOf(node.textValue());
            } else if(node.isNumber()) {
                result = node.longValue();
            }
        }
        return result;
    }

    public static boolean getBooleanValue(JsonNode node, boolean defVal) {
        boolean result = defVal;
        if(node!=null && !node.isNull()) {
            if(node.isBoolean()) {
                result =node.booleanValue();
            } else if(node.isTextual()) {
                result = Boolean.valueOf(node.textValue());
            }
        }
        return result;
    }

    public static UUID extractId(JsonNode idNode, UUID defVal) {
        UUID result = defVal;
        if(idNode!=null && !idNode.isNull()) {
            result = UUID.fromString(idNode.textValue());
        } else {
            logger.trace("Cannot extract UUID from node [{}]", idNode);
        }
        return result;
    }

    public static TaskTarget extractTaskTarget(JsonNode targetNode, TaskTarget defVal) {
        TaskTarget result = defVal;
        if(targetNode!=null && !targetNode.isNull()) {

            String taskType = targetNode.get(TASK_TARGET_TYPE).textValue();
            TaskType tasktypeEnumVal = TaskType.valueOf(taskType);
            String taskMethod = targetNode.get(TASK_TARGET_METHOD).textValue();
            String taskName = targetNode.get(TASK_TARGET_NAME).textValue();
            String taskVersion = targetNode.get(TASK_TARGET_VERSION).textValue();

            result = new TaskTargetImpl(tasktypeEnumVal, taskName, taskVersion, taskMethod);
        } else {
            logger.trace("Cannot extract TaskTarget from node [{}]", targetNode);
        }
        return result;
    }

    public static ArgContainer[] extractArgs(JsonNode argsNode, ArgContainer[] defVal) {
        ArgContainer[] result = defVal;
        if(argsNode!=null && !argsNode.isNull() && argsNode.isArray()) {
            Iterator<JsonNode> argsIterator = argsNode.elements();
            List<ArgContainer> argumentsList = new ArrayList<ArgContainer>();
            while(argsIterator.hasNext()) {
                JsonNode arg = argsIterator.next();

                argumentsList.add(parseArgument(arg));
            }
            result = argumentsList.toArray(new ArgContainer[argumentsList.size()]);
        } else {
            logger.trace("Cannot extract task args from node[{}]", argsNode);
        }

        return result;
    }

    public static TaskOptionsContainer extractOptions(JsonNode optionsNode, TaskOptionsContainer defValue) {
        TaskOptionsContainer result = null;
        if (optionsNode != null && !optionsNode.isNull()) {
            logger.debug("Deserializing taskOptionsContainer node[{}]", optionsNode);
            JsonNode typesNode = optionsNode.get(OPTIONS_ARG_TYPES);
            if (typesNode != null && !typesNode.isNull() && typesNode.isArray()) {
                Iterator<JsonNode> typesIterator = typesNode.elements();
                List<ArgType> argTypes = new ArrayList<ArgType>(typesNode.size());
                while (typesIterator.hasNext()) {
                    JsonNode item = typesIterator.next();
                    if(item.isNumber()) {
                        argTypes.add(ArgType.fromInt(Integer.valueOf(item.intValue())));
                    } else{
                        argTypes.add(ArgType.valueOf(item.textValue()));
                    }

                }
                result = new TaskOptionsContainer(argTypes.toArray(new ArgType[argTypes.size()]));
            }
        }
        return result;
    }

    public static ArgContainer parseArgument(JsonNode arg) {
        ArgContainer result = null;
        if(arg==null || arg.isNull()) {
            return result;
        } else {
            String className = arg.get(ARG_CLASSNAME).textValue();
            Boolean isPromise = arg.get(ARG_IS_PROMISE).booleanValue();
            UUID taskId = extractId(arg.get(ARG_TASK_ID), null);
            Boolean isReady = arg.get(ARG_IS_READY).booleanValue();
            String json = arg.get(ARG_JSON_VALUE).textValue();

            return new ArgContainer(className, isPromise, taskId, isReady, json);
        }
    }

    public static long extractStartTime(JsonNode jsonNode, long defVal) {
        long result = defVal;
        if(jsonNode!=null && !jsonNode.isNull()) {
            result = jsonNode.longValue();
        }
        return result;
    }

    public static int extractNumberOfAttempts(JsonNode jsonNode, int defVal) {
        int result = defVal;
        if(jsonNode!=null && !jsonNode.isNull()) {
            result = jsonNode.intValue();
        }
        return result;
    }

    public static TaskContainer parseTaskContainer(JsonNode rootNode) {
        logger.debug("Deserializing Task from JSON[{}]", rootNode);

        UUID taskId = extractId(rootNode.get(TASK_ID), null);
        UUID processId = extractId(rootNode.get(TASK_PROCESS_ID), null);

        String method = getStringValue(rootNode.get(TASK_METHOD), null);
        String actorId = getStringValue(rootNode.get(TASK_ACTOR_ID), null);
        TaskType type = TaskType.valueOf(getStringValue(rootNode.get(TASK_TYPE), null));

        ArgContainer[] args = extractArgs(rootNode.get(TASK_ARGS), null);
        TaskOptionsContainer options = extractOptions(rootNode.get(TASK_OPTIONS), null);

        long startTime = extractStartTime(rootNode.get(TASK_START_TIME), -1);
        int numberOfAttempts = extractNumberOfAttempts(rootNode.get(TASK_NUMBER_OF_ATTEMPTS), -1);

        return new TaskContainer(taskId, processId, method, actorId, type, startTime, numberOfAttempts, args, options);
    }


    public static ErrorContainer parseErrorContainer(JsonNode rootNode) {
        ErrorContainer result = null;
        if(rootNode!=null && !rootNode.isNull()) {
            result = new ErrorContainer();
            result.setClassName(getStringValue(rootNode.get(ERR_CLASS_NAME), null));
            result.setMessage(getStringValue(rootNode.get(ERR_MESSAGE), null));
            result.setRestartTime(getLongValue(rootNode.get(ERR_RESTART_TIME), -1));
            result.setShouldBeRestarted(getBooleanValue(rootNode.get(ERR_SHOULD_BE_RESTARTED), false));

            JsonNode stNode = rootNode.get(ERR_STACK_TRACE);
            StackTraceElementContainer[] stackTrace = null;
            if(stNode!=null && stNode.isArray()) {
                stackTrace = new StackTraceElementContainer[stNode.size()];
                int pos = 0;
                Iterator<JsonNode> stackTraceElements = stNode.elements();
                while(stackTraceElements.hasNext()) {
                    stackTrace[pos++] = parseStackTraceElementContainer(stackTraceElements.next());
                }
            }
            result.setStackTrace(stackTrace);
        }

        return result;
    }

    public static StackTraceElementContainer parseStackTraceElementContainer(JsonNode rootNode) {
        StackTraceElementContainer result = new StackTraceElementContainer();
        result.setDeclaringClass(getStringValue(rootNode.get(STE_DECLARING_CLASS), null));
        result.setFileName(getStringValue(rootNode.get(STE_FILE_NAME), null));
        result.setLineNumber(getIntegerValue(rootNode.get(STE_LINE_NUMBER), -1));
        result.setMethodName(getStringValue(rootNode.get(STE_METHOD_NAME), null));
        return result;
    }

}

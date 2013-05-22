package ru.taskurotta.adapter.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.ArgContainer;

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

    public ArgContainer dumpArg(Object arg) {

        ArgContainer.ValueType type = ArgContainer.ValueType.PLAIN;
        UUID taskId = null;
        boolean isReady = true;

        ArgContainer result;
        String className = null;
        String jsonValue = null;
        if (arg != null) {
            try {
                if (arg.getClass().isArray()) {
                    className = arg.getClass().getComponentType().getName();
                    if (arg.getClass().getComponentType().isAssignableFrom(Object.class)) {
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

    public ObjectMapper getMapper() {
        return mapper;
    }
}

package ru.taskurotta.server.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.ArgContainer.ValueType;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Common utility static methods set
 * User: dimadin
 * Date: 18.07.13 21:48
 */
public class SerializationUtils {

    private static final Logger logger = LoggerFactory.getLogger(SerializationUtils.class);

    /**
     * @return method with name "add" and single argument
     */
    public static Method getAddMethod(Class collectionClass) {
        Method result = null;
        for (Method method : collectionClass.getDeclaredMethods()) {
            if ("add".equalsIgnoreCase(method.getName()) && method.getParameterTypes().length == 1) {
                result = method;
                break;
            }
        }
        return result;
    }

    public static ValueType extractValueType(Class argClass) {
        ValueType result = null;
        if (argClass.isArray()) {
            result = ValueType.ARRAY;
        } else if (Collection.class.isAssignableFrom(argClass)) {
            result = ValueType.COLLECTION;
        } else {
            result = ValueType.PLAIN;
        }
        logger.debug("ValueType determined for class [{}] is [{}]", argClass, result);
        return result;
    }


}

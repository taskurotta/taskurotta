package ru.taskurotta.server.json;

import ru.taskurotta.transport.model.ArgContainer.ValueType;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Common utility static methods set
 * User: dimadin
 * Date: 18.07.13 21:48
 */
public class SerializationUtils {

    /**
     * @return method with name "add" and single argument
     */
    public static Method getAddMethod(Class collectionClass) {
        Method result = null;
        for(Method method: collectionClass.getDeclaredMethods()) {
            if("add".equalsIgnoreCase(method.getName()) && method.getParameterTypes().length==1) {
                result = method;
                break;
            }
        }
        return result;
    }

    public static ValueType extractValueType(Class argClass) {
        if(argClass.isArray()) {
            return ValueType.ARRAY;
        } else if(Collection.class.isAssignableFrom(argClass)) {
            return ValueType.COLLECTION;
        } else {
            return ValueType.PLAIN;
        }
    }


}

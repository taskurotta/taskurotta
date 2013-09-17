package ru.taskurotta.backend.common;

import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * Created by void 30.04.13 15:58
 */

//todo COPY PASTE FROM server
public class ArrayFactory {

    public static final HashMap<String, Class> primitives = new HashMap<>(8);
    static {
        primitives.put("byte", byte.class);
        primitives.put("short", short.class);
        primitives.put("int", int.class);
        primitives.put("long", long.class);
        primitives.put("float", float.class);
        primitives.put("double", double.class);
        primitives.put("char", char.class);
        primitives.put("boolean", boolean.class);
    }

    public static Object newInstance(String className, int length) throws ClassNotFoundException {
        Class theClass = primitives.get(className);
        if (null == theClass) {
            theClass = Class.forName(className);
        }
        return Array.newInstance(theClass, length);
    }
}

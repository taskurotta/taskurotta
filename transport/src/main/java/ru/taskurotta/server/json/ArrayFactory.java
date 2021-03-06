package ru.taskurotta.server.json;

import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * Created by void 30.04.13 15:58
 */
public class ArrayFactory {

    public static final HashMap<String, Class> primitives = new HashMap<String, Class>(8);
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
        if (theClass == null) {
            theClass = Class.forName(className);
        }
        return Array.newInstance(theClass, length);
    }
}

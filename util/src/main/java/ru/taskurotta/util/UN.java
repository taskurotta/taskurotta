package ru.taskurotta.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class UN {

    private static final Logger logger = LoggerFactory.getLogger(UN.class);

    public static ConcurrentHashMap<Object, List<Throwable>> putRegistry = new ConcurrentHashMap<Object,
            List<Throwable>>(200);

    public static void put(Object id, String msg) {
        List<Throwable> list = new ArrayList<Throwable>();
        List<Throwable> oldList = UN.putRegistry.putIfAbsent(id, list);
        if (oldList != null) {
            list = oldList;
        }

        synchronized (list) {
            list.add(new Throwable(new Date().toString() + " " + System.currentTimeMillis() + "  " + msg));
        }
    }

    public static void print(Object id) {
        List<Throwable> throwableList = UN.putRegistry.get(id);

        if (throwableList == null) {
            logger.error("UUID " + id + " has no registered put operation");
            return;
        }

        int i = 0;
        for (Throwable th: throwableList) {
            ++i;
            logger.error("UUID " + id + " put + " + i + " from", th);
        }
    }

}

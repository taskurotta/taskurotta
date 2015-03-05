package ru.taskurotta.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class UN {

    private static final Logger logger = LoggerFactory.getLogger(UN.class);

    public static ConcurrentHashMap<UUID, List<Throwable>> putRegistry = new ConcurrentHashMap<UUID, List<Throwable>>(200);

    public static void put(UUID id) {
        List<Throwable> list = new ArrayList<Throwable>();
        List<Throwable> oldList = UN.putRegistry.putIfAbsent(id, list);
        if (oldList != null) {
            list = oldList;
        }
        list.add(new Throwable("" + System.currentTimeMillis()));
    }

    public static void print(UUID id) {
        List<Throwable> throwableList = UN.putRegistry.get(id);

        if (throwableList == null) {
            logger.error("UUID " + id + " has no registered put operation");
            return;
        }

        int i = 0;
        for (Throwable th: throwableList) {
            logger.error("UUID " + id + " put + " + (++i) + " from", th);
        }
    }

}

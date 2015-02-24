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

    public static ConcurrentHashMap<UUID, List<Throwable>> tasksToQueue = new ConcurrentHashMap<UUID, List<Throwable>>(200);
    public static ConcurrentHashMap<UUID, Object> tasksToResurrect = new ConcurrentHashMap<UUID, Object>(200);

    public static void put(UUID taskId) {
        List<Throwable> list = new ArrayList<Throwable>();
        List<Throwable> oldList = UN.tasksToQueue.putIfAbsent(taskId, list);
        if (oldList != null) {
            list = oldList;
        }
        list.add(new Throwable("" + System.currentTimeMillis()));
    }

    public static void print(UUID taskId) {
        List<Throwable> throwableList = UN.tasksToQueue.get(taskId);

        int i = 0;
        for (Throwable th: throwableList) {
            logger.error("Task " + taskId + " enqueue + " + (++i) + " is", th);
        }
    }

}

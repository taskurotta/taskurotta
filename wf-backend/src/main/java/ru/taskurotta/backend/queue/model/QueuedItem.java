package ru.taskurotta.backend.queue.model;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 12:29 PM
 */
public class QueuedItem {

    private UUID taskId;
    private long startTime;

    public QueuedItem(UUID taskId, long startTime) {
        this.taskId = taskId;
        this.startTime = startTime;
    }

    public UUID getTaskId() {
        return taskId;
    }
}

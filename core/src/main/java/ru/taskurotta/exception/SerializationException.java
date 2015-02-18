package ru.taskurotta.exception;

import java.util.UUID;

/**
 * Error at (de)serializing container entities
 * User: dimadin
 * Date: 18.07.13 16:27
 */
public class SerializationException extends RuntimeException {

    UUID taskId;
    UUID processId;

    public SerializationException() {
    }

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }
}

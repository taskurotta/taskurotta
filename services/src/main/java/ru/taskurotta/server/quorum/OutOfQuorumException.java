package ru.taskurotta.server.quorum;

/**
 */
public class OutOfQuorumException extends RuntimeException {

    public OutOfQuorumException(String message) {
        super(message);
    }
}

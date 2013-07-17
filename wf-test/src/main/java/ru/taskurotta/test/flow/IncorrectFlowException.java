package ru.taskurotta.test.flow;

/**
 * Created by void 29.03.13 15:45
 */
public class IncorrectFlowException extends RuntimeException {
    public IncorrectFlowException() {
    }

    public IncorrectFlowException(String message) {
        super(message);
    }

    public IncorrectFlowException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncorrectFlowException(Throwable cause) {
        super(cause);
    }

}

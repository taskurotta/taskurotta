package ru.taskurotta.test.flow;

import ru.taskurotta.exception.test.TestException;

/**
 * Created by void 29.03.13 15:45
 */
public class IncorrectFlowException extends TestException {

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

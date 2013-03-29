package ru.taskurotta.test;

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

	public IncorrectFlowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}

package ru.taskurotta.exception;

/**
 * created by void 23.01.13 12:58
 */
public class ProxyFactoryException extends ActorRuntimeException {
	public ProxyFactoryException() {
	}

	public ProxyFactoryException(String message) {
		super(message);
	}

	public ProxyFactoryException(String message, Throwable cause) {
		super(message, cause);
	}
}

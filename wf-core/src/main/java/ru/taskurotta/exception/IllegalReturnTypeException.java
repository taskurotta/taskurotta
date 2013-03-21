package ru.taskurotta.exception;

/**
 * User: stukushin
 * Date: 24.01.13
 * Time: 17:58
 */
public class IllegalReturnTypeException extends IllegalClientImplementationException {
    public IllegalReturnTypeException() {
        super("Can return only Promise objects");
    }
}

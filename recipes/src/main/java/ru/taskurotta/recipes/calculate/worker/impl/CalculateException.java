package ru.taskurotta.recipes.calculate.worker.impl;

/**
 * Custom test service exception
 * Date: 16.12.13 10:25
 */
public class CalculateException extends RuntimeException {

    public CalculateException (String message) {
        super(message);
    }

}

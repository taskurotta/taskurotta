package ru.taskurotta.test.monkey;

/**
 * Created by void 16.04.13 13:59
 */
public class CrazyException extends RuntimeException {

    public static final String SORRY = "Sorry, dog was crazy. I must to shut'em. ";

    public CrazyException() {
        super(SORRY);
    }

    public CrazyException(String message) {
        super(SORRY + message);
    }
}

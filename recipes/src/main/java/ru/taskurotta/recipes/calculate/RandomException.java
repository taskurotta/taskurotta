package ru.taskurotta.recipes.calculate;


import ru.taskurotta.recipes.calculate.worker.impl.CalculateException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

public class RandomException extends RuntimeException {

    private static final long serialVersionUID = -5798044531293640942L;

    public RandomException(String message) {
        super(message);
    }

    public static boolean isEventHappened(double possibility) {
        double random = Math.random();
        return possibility >= (random == 0 ? 1 : random);
    }

    public static Exception getRandomException() {
        Exception result = null;
        long time = System.currentTimeMillis();

        if (time % 5 == 0) {
            result = new IOException("Cannot find some file [" + time + ".txt]");

        } else if (time % 5 == 1) {
            result = new TimeoutException("Some timeout[" + time + "] expired!");

        } else if (time % 5 == 2) {
            result = new NullPointerException("There is a code NPE error! Line [" + time + "]");

        } else if (time % 5 == 3) {
            result = new SQLException("Database error for sql[select [" + time + "] from dual]");

        } else if (time % 5 == 4) {
            result = new CalculateException("It is custom exception with very long text value. It is custom exception with very long text value. It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.It is custom exception with very long text value.");

        } else {
            result = new IllegalStateException("This exception is impossible");
        }

        return result;
    }


}

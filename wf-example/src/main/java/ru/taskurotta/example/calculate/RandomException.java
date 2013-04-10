package ru.taskurotta.example.calculate;

import ru.taskurotta.exception.Retriable;

public class RandomException extends RuntimeException implements Retriable {

    private static final long serialVersionUID = -5798044531293640942L;

    public RandomException(String message){
        super(message);
    }

    public static boolean isEventHappened(double possibility) {
        double random = Math.random();
        return possibility >= (random==0? 1: random);
    }

    @Override
    public boolean isShouldBeRestarted() {
        return true;
    }

    @Override
    public long getRestartTime() {
        return 0;
    }



}

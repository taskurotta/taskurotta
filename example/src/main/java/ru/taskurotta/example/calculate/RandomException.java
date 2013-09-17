package ru.taskurotta.example.calculate;


public class RandomException extends RuntimeException {

    private static final long serialVersionUID = -5798044531293640942L;

    public RandomException(String message){
        super(message);
    }

    public static boolean isEventHappened(double possibility) {
        double random = Math.random();
        return possibility >= (random==0? 1: random);
    }

}

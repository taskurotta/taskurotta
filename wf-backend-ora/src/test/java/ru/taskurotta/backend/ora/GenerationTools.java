package ru.taskurotta.backend.ora;

import java.util.Random;

/**
 * User: greg
 */
public final class GenerationTools {

    private static Random rnd = new Random();

    private GenerationTools() {
    }

    public static synchronized int getRandomType() {
        int num = rnd.nextInt();
        return Math.abs(num % 4);
    }

}

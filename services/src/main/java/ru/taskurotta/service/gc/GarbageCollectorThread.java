package ru.taskurotta.service.gc;

/**
 * User: stukushin
 * Date: 06.10.2015
 * Time: 17:10
 */

public interface GarbageCollectorThread {

    void start();

    void stop();

    boolean isStarted();

}

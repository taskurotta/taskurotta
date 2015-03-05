package ru.taskurotta.service.recovery;

/**
 * User: stukushin
 * Date: 18.12.13
 * Time: 14:48
 */
public interface RecoveryThreads {

    public void start();

    public void stop();

    public boolean isStarted();

}

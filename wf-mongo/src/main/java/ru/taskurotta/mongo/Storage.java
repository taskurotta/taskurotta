package ru.taskurotta.mongo;

/**
 * User: stukushin
 * Date: 27.12.12
 * Time: 14:08
 */
public interface Storage {
    public void saveToJournal(Object task) throws InterruptedException;
}

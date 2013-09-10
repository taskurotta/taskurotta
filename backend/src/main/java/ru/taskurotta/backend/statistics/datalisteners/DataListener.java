package ru.taskurotta.backend.statistics.datalisteners;

/**
 * User: stukushin
 * Date: 26.08.13
 * Time: 19:01
 */
public interface DataListener {
    public void handle(String name, long count, double value, long time);

    public long[] getHourCount();

    public long[] getDayCount();
}

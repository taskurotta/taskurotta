package ru.taskurotta.backend.statistics.metrics;

/**
 * User: stukushin
 * Date: 04.09.13
 * Time: 10:53
 */
public interface CheckPoint {
    public void mark(long period);
    public void dump();
}

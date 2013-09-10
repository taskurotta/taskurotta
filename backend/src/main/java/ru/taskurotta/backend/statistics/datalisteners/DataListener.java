package ru.taskurotta.backend.statistics.datalisteners;

/**
 * User: stukushin
 * Date: 10.09.13
 * Time: 18:30
 */
public interface DataListener {
    void handle(String name, long count, double mean, long time);

    long[] getHourCounts();

    long[] getDayCounts();

    double[] getHourMeans();

    double[] getDayMeans();
}

package ru.taskurotta.backend.statistics;

/**
 * Persistance support for metrics data
 * User: dimadin
 * Date: 15.09.13 12:23
 */
public interface MetricsDataStore {

    public void save();

    public void load();

}

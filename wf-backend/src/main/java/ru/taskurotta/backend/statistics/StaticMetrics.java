package ru.taskurotta.backend.statistics;

/**
 * User: romario
 * Date: 8/14/13
 * Time: 7:34 PM
 */
public class StaticMetrics {

    private static final Metrics metrics = new Metrics();

    public static Metrics.CheckPoint create(String name) {
        return metrics.create(name);
    }

    public static void setDataListener(Metrics.DataListener dataListener) {
        metrics.setDataListener(dataListener);
    }

}

package ru.taskurotta.dropwizard.resources.console.metrics.support;

/**
 * Constant values used by metrics visualizer
 * User: dimadin
 * Date: 09.09.13 16:17
 */
public interface MetricsConstants {

    public static final String ACTION_METRICS_DATA = "data";
    public static final String ACTION_METRICS_OPTIONS = "options";

    public static final String OPT_SCOPE_CLUSTER = "cluster";
    public static final String OPT_SCOPE_LOCAL = "local";

    public static final String OPT_PERIOD_HOUR = "hour";
    public static final String OPT_PERIOD_DAY = "day";

    public static final String OPT_DATATYPE_RATE = "rate";
    public static final String OPT_DATATYPE_MEAN = "mean";

    public static final String OPT_UNDEFINED = "-1";

}

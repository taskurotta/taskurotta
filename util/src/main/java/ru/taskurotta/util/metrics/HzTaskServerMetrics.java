package ru.taskurotta.util.metrics;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;

import java.util.concurrent.TimeUnit;

/**
 */
public class HzTaskServerMetrics {

    public static final Timer STAT = Metrics.newTimer(HzTaskServerMetrics.class, "release",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

    public static final Timer statRelease = Metrics.newTimer(HzTaskServerMetrics.class, "release",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

    // processDecision start
    public static final Timer statPdAll = Metrics.newTimer(HzTaskServerMetrics.class, "process decision all",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

    public static final Timer statPdLock = Metrics.newTimer(HzTaskServerMetrics.class, "process decision lock",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

    public static final Timer statPdWork = Metrics.newTimer(HzTaskServerMetrics.class, "process decision work",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

}

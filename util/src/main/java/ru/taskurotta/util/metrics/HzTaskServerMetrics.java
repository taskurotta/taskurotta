package ru.taskurotta.util.metrics;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;

import java.util.concurrent.TimeUnit;

/**
 */
public class HzTaskServerMetrics {

    public static Timer storeTimer = Metrics.newTimer(HzTaskServerMetrics.class, "store",
            TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

}

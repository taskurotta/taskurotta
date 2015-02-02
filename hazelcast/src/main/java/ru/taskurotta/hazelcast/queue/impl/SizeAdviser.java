package ru.taskurotta.hazelcast.queue.impl;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import ru.taskurotta.hazelcast.queue.config.CachedQueueSizeConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 */
public class SizeAdviser {

    public static final String EVENT_POLL_NOT_NULL = "pollNotNUll";
    public static final String EVENT_OFFER = "offer";
    public static final int DEFAULT_SIZE = 100;

    private String hzInstanceName;
    private Map<String, QueueStats> name2QueueStatsMap = new ConcurrentHashMap<>();


    public static class QueueStats {

        // todo: started size should be configurable
        int currentSize = DEFAULT_SIZE;
        int recommendedSize = DEFAULT_SIZE;

        Meter pollNotNullMeter;
        Timer offerTimer;

        public QueueStats(String hzInstanceName, String queueName) {
            pollNotNullMeter = getPollNotNullMeter(hzInstanceName, queueName);
            offerTimer = getOfferTimer(hzInstanceName, queueName);
        }
    }

    public SizeAdviser(String hzInstanceName, final CachedQueueSizeConfig sizeConfig) {
        this.hzInstanceName = hzInstanceName;


        new Thread() {
            @Override
            public void run() {

                while (true) {

                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException ignore) {
                    }


                    int sumNotNullRate = 0;

                    for (QueueStats queueStats : name2QueueStatsMap.values()) {
                        sumNotNullRate += queueStats.pollNotNullMeter.fiveMinuteRate();
                    }

                    if (sumNotNullRate == 0) {
                        continue;
                    }

                    int size = sizeConfig.getSize();
                    long totalBytesQuota = (long) (1D * Runtime.getRuntime().totalMemory() / 100 * size);

                    for (QueueStats queueStats : name2QueueStatsMap.values()) {
                        int queueRate = (int) queueStats.pollNotNullMeter.fiveMinuteRate();

                        queueRate = queueRate == 0 ? 1 : queueRate;

                        long itemSize = (long) queueStats.offerTimer.mean();

                        if (itemSize == 0) {
                            queueStats.recommendedSize = DEFAULT_SIZE;
                            continue;
                        }

                        queueStats.recommendedSize = (int) (1D * queueRate / sumNotNullRate * totalBytesQuota / itemSize);
                    }

                }

            }
        }.start();
    }

    public static Meter getPollNotNullMeter(String hzInstanceName, String queueName) {
        return Metrics.newMeter(SizeAdviser.class, queueName + '$' + EVENT_POLL_NOT_NULL, hzInstanceName, TimeUnit
                .SECONDS);
    }

    public static Timer getOfferTimer(String hzInstanceName, String queueName) {
        return Metrics.newTimer(SizeAdviser.class, queueName + '$' + EVENT_OFFER, hzInstanceName, TimeUnit.NANOSECONDS,
                TimeUnit.SECONDS);
    }

    public int getRecommendedSize(String queueName) {
        QueueStats queueStats = name2QueueStatsMap.get(queueName);
        return queueStats.recommendedSize;
    }

    public void addQueue(String queueName) {

        QueueStats queueStats = new QueueStats(hzInstanceName, queueName);

        name2QueueStatsMap.put(queueName, queueStats);

    }

    public void removeQueue(String queueName) {
        name2QueueStatsMap.remove(queueName);
    }

}

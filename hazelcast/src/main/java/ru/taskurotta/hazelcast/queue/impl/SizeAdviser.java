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


                    long sumRateInBytes = 0;

                    for (QueueStats queueStats : name2QueueStatsMap.values()) {
                        sumRateInBytes +=
                                queueStats.pollNotNullMeter.oneMinuteRate() * (long) queueStats.offerTimer.mean();
                    }

                    if (sumRateInBytes == 0) {
                        continue;
                    }


                    int size = sizeConfig.getSize();
                    long totalBytesQuota = (long) (1D * Runtime.getRuntime().totalMemory() / 100 * size);
                    double realK = 1D * totalBytesQuota / sumRateInBytes;

                    for (QueueStats queueStats : name2QueueStatsMap.values()) {
                        int queueRate = (int) queueStats.pollNotNullMeter.oneMinuteRate();

                        queueRate = queueRate == 0 ? 1 : queueRate;

                        long itemSize = (long) queueStats.offerTimer.mean();

                        if (itemSize == 0) {
                            queueStats.recommendedSize = DEFAULT_SIZE;
                            continue;
                        }

                        int recommendedSize = (int) (realK * queueRate);
                        if (recommendedSize < DEFAULT_SIZE) {
                            recommendedSize = DEFAULT_SIZE;
                        }

                        queueStats.recommendedSize = recommendedSize;
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


    // algorithm
    public static void main(String[] args) {

        long totalBytesQuota = 10 * 1024 * 1024;

        int[] queueRate = new int[]{1200, 200, 500};
        int[] itemBytes = new int[]{350, 86, 86};
        int[] queueSize = new int[queueRate.length];

        long sumRateInBytes = 0;
        for (int i = 0; i < queueRate.length; i++) {
            sumRateInBytes += queueRate[i] * itemBytes[i];
        }

        double realK = 1D * totalBytesQuota / sumRateInBytes;

        long totalBytesOfNewQueues = 0;
        for (int i = 0; i < queueRate.length; i++) {

            queueSize[i] = (int) (queueRate[i] * realK);
            totalBytesOfNewQueues += queueSize[i]* itemBytes[i];

            System.err.println("queue [" + i + "] should have size " + queueSize[i]);

        }

        System.err.println("Total new size = " + totalBytesOfNewQueues);

    }
}

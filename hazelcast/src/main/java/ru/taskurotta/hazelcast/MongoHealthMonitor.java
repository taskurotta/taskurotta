package ru.taskurotta.hazelcast;

import com.mongodb.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.hazelcast.store.MongoMapStore;
import ru.taskurotta.hazelcast.store.MongoQueueStore;

/**
 * Periodically prints metrics data and/or mongo server status to INFO log
 *
 * Date: 03.02.14 18:29
 */
public class MongoHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(MongoHealthMonitor.class);

    private long metricsPeriodMs;
    private long mongoStatPeriodMs;

    private MongoTemplate mongoTemplate;

    public MongoHealthMonitor(MongoTemplate mongoTemplate, long mongoStatPeriodMs, long metricsPeriodMs) {

        this.mongoTemplate = mongoTemplate;
        this.mongoStatPeriodMs = mongoStatPeriodMs;
        this.metricsPeriodMs = metricsPeriodMs;

        if (metricsPeriodMs > 0) {//Logging metrics info
            runMetricsMonitorThread("MongoMapStore-monitor#"+getClass().getName(), metricsPeriodMs);
        }

        if (mongoStatPeriodMs > 0) {//Mongo server stat info
            runMongoStatMonitorThread("MongoServerStatus-monitor#"+getClass().getName(), mongoStatPeriodMs);
        }

    }

    private void runMetricsMonitorThread(String name, final long sleepMs) {

        Thread monitor = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    while(true) {

                        logger.info(getMetricsStats());

                        Thread.sleep(sleepMs);
                    }
                } catch (Throwable e) {
                    logger.debug("Stopping debug monitor due to error", e);
                }

            }
        });
        monitor.setName(name);
        monitor.setDaemon(true);
        monitor.start();

    }

    public String getMetricsStats() {
        StringBuilder sb = new StringBuilder("\nMongoMapStore statistics:");

        sb.append(String.format("\ndelete count: %d mean: %8.3f oneMinuteRate: %8.3f",
                MongoMapStore.deleteTimer.count(), MongoMapStore.deleteTimer.mean(), MongoMapStore.deleteTimer.oneMinuteRate()));
        sb.append(String.format("\nload count: %d mean: %8.3f oneMinuteRate: %8.3f",
                MongoMapStore.loadTimer.count(), MongoMapStore.loadTimer.mean(), MongoMapStore.loadTimer.oneMinuteRate()));
        sb.append(String.format("\nstore count: %d mean: %8.3f oneMinuteRate: %8.3f",
                MongoMapStore.storeTimer.count(), MongoMapStore.storeTimer.mean(), MongoMapStore.storeTimer.oneMinuteRate()));

        sb.append("\nMongo Queues statistics:");
        sb.append(String.format("\ndelete count: %d mean: %8.3f oneMinuteRate: %8.3f",
                MongoQueueStore.deleteTimer.count(), MongoQueueStore.deleteTimer.mean(), MongoQueueStore.deleteTimer.oneMinuteRate()));
        sb.append(String.format("\nload count: %d mean: %8.3f oneMinuteRate: %8.3f",
                MongoQueueStore.deleteTimer.count(), MongoQueueStore.loadTimer.mean(), MongoQueueStore.loadTimer.oneMinuteRate()));
        sb.append(String.format("\nstore count: %d mean: %8.3f oneMinuteRate: %8.3f",
                MongoQueueStore.deleteTimer.count(), MongoQueueStore.storeTimer.mean(), MongoQueueStore.storeTimer.oneMinuteRate()));

        return sb.toString();
    }

    public String getMongoServerStats() {
        CommandResult cr = mongoTemplate.executeCommand("{serverStatus: 1}");
        StringBuilder sb = new StringBuilder("\nMongo server stat:");
        sb.append(cr.toString());

        return sb.toString();
    }

    private void runMongoStatMonitorThread(String name, final long sleepMs) {

        Thread monitor = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    while(true) {

                        logger.info(getMongoServerStats());

                        Thread.sleep(sleepMs);
                    }
                } catch (Throwable e) {
                    logger.debug("Stopping debug monitor due to error", e);
                }

            }
        });
        monitor.setName(name);
        monitor.setDaemon(true);
        monitor.start();

    }

}

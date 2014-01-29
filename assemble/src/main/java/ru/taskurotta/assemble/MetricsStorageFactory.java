package ru.taskurotta.assemble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.delay.Storage;
import ru.taskurotta.hazelcast.queue.delay.StorageFactory;
import ru.taskurotta.service.metrics.Metric;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.MetricsFactory;

import java.util.concurrent.TimeUnit;

/**
 * StorageFactory metrics decorator
 * Date: 28.01.14 14:52
 */
public class MetricsStorageFactory implements StorageFactory {

    private StorageFactory storageFactory;
    private MetricsFactory metricsFactory;

    private static final Logger logger = LoggerFactory.getLogger(MetricsStorageFactory.class);

    public MetricsStorageFactory(StorageFactory storageFactory, MetricsFactory metricsFactory) {
        this.storageFactory = storageFactory;
        this.metricsFactory = metricsFactory;
    }

    @Override
    public Storage createStorage(String queueName) {
        final Storage storage = storageFactory.createStorage(queueName);
        final Metric storageMetric = metricsFactory.getInstance(MetricName.STORAGE.getValue());

        logger.debug("Creating new [{}] metric for queue[{}] with original storage class[{}]", MetricName.STORAGE.getValue(), queueName, storage.getClass().getName());

        return new Storage() {

            @Override
            public boolean add(Object o, long delayTime, TimeUnit unit) {
                long start = System.currentTimeMillis();
                boolean result = storage.add(o, delayTime, unit);
                long period = System.currentTimeMillis() - start;
                storageMetric.mark("add", period);
                storageMetric.mark(MetricName.STORAGE.getValue(), period);
                logger.debug("Marking add metric");
                return result;
            }

            @Override
            public boolean remove(Object o) {
                long start = System.currentTimeMillis();
                boolean result = storage.remove(o);
                long period = System.currentTimeMillis() - start;
                storageMetric.mark("remove", period);
                storageMetric.mark(MetricName.STORAGE.getValue(), period);
                logger.debug("Marking remove metric");
                return result;
            }

            @Override
            public void clear() {
                long start = System.currentTimeMillis();
                storage.clear();
                long period = System.currentTimeMillis() - start;
                storageMetric.mark("clear", period);
                storageMetric.mark(MetricName.STORAGE.getValue(), period);
                logger.debug("Marking clear metric");
            }

            @Override
            public void destroy() {
                long start = System.currentTimeMillis();
                storage.destroy();
                long period = System.currentTimeMillis() - start;
                storageMetric.mark("destroy", period);
                storageMetric.mark(MetricName.STORAGE.getValue(), period);
                logger.debug("Marking destroy metric");
            }

        };
    }
}

package ru.taskurotta.service.hz.support;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.handler.NumberDataListener;
import ru.taskurotta.service.metrics.TimeConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Helper class providing info on hazelcast queues size
 * Date: 26.12.13 12:31
 */
public class HzQueueSizeMetricProvider {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueSizeMetricProvider.class);

    private HazelcastInstance hzInstance;
    private NumberDataListener numberDataListener;
    private QueueInfoRetriever retriever;
    private String queueNamePrefix;

    public HzQueueSizeMetricProvider(HazelcastInstance hzInstance, NumberDataListener numberDataListener, QueueInfoRetriever retriever, String queueNamePrefix, int queueSizeMetricPeriodSeconds) {
        this.hzInstance = hzInstance;
        this.numberDataListener = numberDataListener;
        this.retriever = retriever;
        this.queueNamePrefix = queueNamePrefix;

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        //Queue statistics for metrics
        int dataPointsCount = TimeConstants.SECONDS_IN_24_HOURS/Long.valueOf(queueSizeMetricPeriodSeconds).intValue()
                ;//number of points to cover 24 hours period.
        scheduledExecutorService.scheduleAtFixedRate(new QueueSizeDataFlusher(dataPointsCount), 0,
                queueSizeMetricPeriodSeconds, TimeUnit.SECONDS);

    }

    class QueueSizeDataFlusher implements Runnable {
        private int dataSize = 0;

        public QueueSizeDataFlusher (int dataSize) {
            this.dataSize = dataSize;
        }

        @Override
        public void run() {
            try {
                int count = 0;
                int totalSize = 0;
                if (numberDataListener != null) {
                    for (String queue : getQueueNames()) {
                        int queueSize = retriever.getQueueTaskCount(queue);
                        numberDataListener.handleNumberData(MetricName.QUEUE_SIZE.getValue(), queue, queueSize,
                                System.currentTimeMillis(), dataSize);
                        count++;
                        totalSize+=queueSize;
                    }
                    numberDataListener.handleNumberData(MetricName.QUEUE_SIZE.getValue(),
                            MetricName.QUEUE_SIZE.getValue(), totalSize, System.currentTimeMillis(), dataSize);
                }
                logger.debug("Queue size data items [{}] successfully flushed", count);

            } catch (Throwable e) {
                logger.error("QueueDataFlusher iteration failed", e);
            }
        }

        private List<String> getQueueNames() {
            List<String> result = new ArrayList<>();
            for (DistributedObject inst : hzInstance.getDistributedObjects()) {
                if (inst instanceof IQueue) {
                    String name = inst.getName();
                    if (name.startsWith(queueNamePrefix)) {
                        result.add(name);
                    }
                }
            }
            return result;
        }

    }


}

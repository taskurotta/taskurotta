package ru.taskurotta.service.hz.metrics;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.MetricsFactory;
import ru.taskurotta.service.metrics.PeriodicMetric;
import ru.taskurotta.service.metrics.PeriodicMetric.DatasetValueExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class providing info on hazelcast queues size
 * Date: 26.12.13 12:31
 */
public class HzQueueSizeMetricProvider {

    public HzQueueSizeMetricProvider(final HazelcastInstance hzInstance, MetricsFactory metricsFactory, final QueueInfoRetriever retriever, final String queueNamePrefix, int queueSizeMetricPeriodSeconds) {

        PeriodicMetric queueSizeMetric = metricsFactory.getPeriodicInstance(MetricName.QUEUE_SIZE.getValue(), queueSizeMetricPeriodSeconds);

        queueSizeMetric.periodicMark(new DatasetValueExtractor() {

            @Override
            //return list of queue names
            public List<String> getDatasets() {
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

            @Override
            //returns number of tasks in a queue
            public Number getDatasetValue(String dataset) {
                return retriever.getQueueTaskCount(dataset);
            }

            @Override
            //returns sum of queue sizes for this metric
            public Number getGeneralValue(Map<String, Number> datasetsValues) {
                int result = 0;
                if (datasetsValues != null && !datasetsValues.isEmpty()) {
                    for (Number num: datasetsValues.values()) {
                        if (num != null) {
                            result += num.intValue();
                        }
                    }
                }
                return result;
            }
        });
    }

}

package ru.taskurotta.backend.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.recovery.AbstractQueueBackendStatistics;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 18.09.13
 * Time: 11:50
 */
public class HzQueueBackendStatistics extends AbstractQueueBackendStatistics {

    private HazelcastInstance hzInstance;

    public static final String lastPolledTaskEnqueueTimesName = "lastPolledTaskEnqueueTimes";

    class StatisticsMerger implements Runnable {
        @Override
        public void run() {
            IMap<String, Long> iMap = hzInstance.getMap(lastPolledTaskEnqueueTimesName);

            Set<Map.Entry<String, Long>> entries = lastPolledTaskEnqueueTimes.entrySet();
            for (Map.Entry<String, Long> entry : entries) {
                String queueName = entry.getKey();
                Long lastEnqueueTime = entry.getValue();

                Long previousEnqueueTime = iMap.get(queueName);
                if (previousEnqueueTime == null) {
                    iMap.put(queueName, lastEnqueueTime);
                } else {
                    if (previousEnqueueTime < lastEnqueueTime) {
                        iMap.put(queueName, lastEnqueueTime);
                    }
                }
            }
        }
    }

    public HzQueueBackendStatistics(QueueBackend queueBackend, HazelcastInstance hzInstance, long mergePeriod, TimeUnit mergePeriodTimeUnit) {
        super(queueBackend);
        this.hzInstance = hzInstance;

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new StatisticsMerger(), 0, mergePeriod, mergePeriodTimeUnit);
    }
}

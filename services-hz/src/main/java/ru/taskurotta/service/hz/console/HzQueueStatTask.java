package ru.taskurotta.service.hz.console;

import com.hazelcast.spring.context.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.hz.queue.HzQueueService;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Task that computes queue statistics data by metrics handlers.
 * Runs on every HZ node
 * Date: 29.11.13 16:01
 */
@SpringAware
public class HzQueueStatTask implements Callable<List<QueueStatVO>>, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueStatTask.class);
    private ArrayList<String> queueNames;
    private String queueNamePrefix;
    private HzQueueService hzQueueService;

    public HzQueueStatTask(ArrayList<String> queueNames, String queueNamePrefix) {
        this.queueNames = queueNames;
        this.queueNamePrefix = queueNamePrefix;
    }

    @Autowired
    public void setHzQueueService(HzQueueService hzQueueService) {
        this.hzQueueService = hzQueueService;
    }

    @Override
    public List<QueueStatVO> call() throws Exception {
        logger.debug("Started HzQueueStatTask with queueNames [{}]", this.queueNames);

        List<QueueStatVO> result = null;
        if (queueNames != null && !queueNames.isEmpty()) {
            result = new ArrayList<>();
            MetricsDataHandler mdh = MetricsDataHandler.getInstance();
            NumberDataHandler ndh = NumberDataHandler.getInstance();
            if (mdh != null && ndh != null) {
                for (String queueName : queueNames) {
                    QueueStatVO item = new QueueStatVO();
                    item.setName(queueName);

                    Number count = ndh.getLastValue(MetricName.QUEUE_SIZE.getValue(), queueName);
                    item.setCount(count != null ? (Integer) count : 0);
                    item.setLastActivity(mdh.getLastActivityTime(MetricName.POLL.getValue(), queueName));

                    item.setInHour(mdh.getTotalCountOfLastHour(MetricName.ENQUEUE.getValue(), queueName));
                    item.setOutHour(mdh.getTotalCountOfLastHour(MetricName.SUCCESSFUL_POLL.getValue(), queueName));

                    item.setInDay(mdh.getTotalCountOfLastDay(MetricName.ENQUEUE.getValue(), queueName));
                    item.setOutDay(mdh.getTotalCountOfLastDay(MetricName.SUCCESSFUL_POLL.getValue(), queueName));

                    result.add(item);

                    item.setLastPolledTaskEnqueueTime(hzQueueService.getLastPolledTaskEnqueueTime(queueName));
                }
            } else {
                logger.error("Cannot extract dataHandlers, methodDataHandler[" + mdh + "], numberDataHandler[" + ndh + "]");
            }

        }
        logger.debug("Result list of QueueStatVO is [{}]", result);
        return result;
    }

    public ArrayList<String> getQueueNames() {
        return queueNames;
    }

    public void setQueueNames(ArrayList<String> queueNames) {
        this.queueNames = queueNames;
    }

    public String getQueueNamePrefix() {
        return queueNamePrefix;
    }

    public void setQueueNamePrefix(String queueNamePrefix) {
        this.queueNamePrefix = queueNamePrefix;
    }
}

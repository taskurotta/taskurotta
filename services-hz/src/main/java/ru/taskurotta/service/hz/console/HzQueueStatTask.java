package ru.taskurotta.service.hz.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.handler.NumberDataHandler;
import ru.taskurotta.service.metrics.MetricsDataUtils;
import ru.taskurotta.service.metrics.model.DataPointVO;
import ru.taskurotta.util.ActorUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Task that computes queue statistics data by metrics handlers.
 * Runs on every HZ node
 * Date: 29.11.13 16:01
 */
public class HzQueueStatTask implements Callable<List<QueueStatVO>>, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueStatTask.class);
    private ArrayList<String> queueNames;
    private String queueNamePrefix;

    public HzQueueStatTask(ArrayList<String> queueNames, String queueNamePrefix) {
        this.queueNames = queueNames;
        this.queueNamePrefix = queueNamePrefix;
    }

    @Override
    public List<QueueStatVO> call() throws Exception {
        logger.debug("Started HzQueueStatTask with queueNames [{}]", this.queueNames);

        List<QueueStatVO> result = null;
        if (queueNames!=null && !queueNames.isEmpty()) {
            result = new ArrayList<>();
            MetricsDataHandler mdh = MetricsDataHandler.getInstance();
            NumberDataHandler ndh = NumberDataHandler.getInstance();
            if (mdh != null && ndh != null) {
                for (String queueName: queueNames) {
                    QueueStatVO item = new QueueStatVO();
                    item.setName(queueName);

                    Number count = ndh.getLastValue(MetricName.QUEUE_SIZE.getValue(), ActorUtils.toPrefixed(queueName, queueNamePrefix));
                    item.setCount(count!=null? (Integer)count: 0);
                    item.setLastActivity(mdh.getLastActivityTime(MetricName.POLL.getValue(), queueName));

                    DataPointVO<Long>[] outHour = mdh.getCountsForLastHour(MetricName.SUCCESSFUL_POLL.getValue(), queueName);
                    DataPointVO<Long>[] outDay = mdh.getCountsForLastDay(MetricName.SUCCESSFUL_POLL.getValue(), queueName);

                    DataPointVO<Long>[] inHour = mdh.getCountsForLastHour(MetricName.ENQUEUE.getValue(), queueName);
                    DataPointVO<Long>[] inDay = mdh.getCountsForLastDay(MetricName.ENQUEUE.getValue(), queueName);

                    item.setInDay(MetricsDataUtils.sumUpDataPointsArray(inDay));
                    item.setInHour(MetricsDataUtils.sumUpDataPointsArray(inHour));

                    item.setOutDay(MetricsDataUtils.sumUpDataPointsArray(outDay));
                    item.setOutHour(MetricsDataUtils.sumUpDataPointsArray(outHour));

                    result.add(item);
                }
            } else {
                logger.error("Cannot extract dataHandlers, methodDataHandler["+mdh+"], numberDataHandler["+ndh+"]");
            }

        }
        logger.debug("Result list of QueueStatVO is [{}]", result);
        return result;
    }

}

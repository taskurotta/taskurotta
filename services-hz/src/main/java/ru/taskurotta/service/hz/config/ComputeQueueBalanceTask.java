package ru.taskurotta.service.hz.config;

import ru.taskurotta.service.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.service.metrics.model.DataPointVO;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.model.QueueBalanceVO;
import ru.taskurotta.service.metrics.MetricName;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 01.10.13 16:28
 */
public class ComputeQueueBalanceTask implements Callable<QueueBalanceVO>, Serializable {

    private String queueName;

    public ComputeQueueBalanceTask(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public QueueBalanceVO call() throws Exception {
        MetricsMethodDataRetriever metricsDataRetriever = MetricsDataHandler.getInstance();
        if (metricsDataRetriever == null) {
            return null;
        }

        DataPointVO<Long>[] outHour = metricsDataRetriever.getCountsForLastHour(MetricName.SUCCESSFUL_POLL.getValue(), queueName);
        DataPointVO<Long>[] outDay = metricsDataRetriever.getCountsForLastDay(MetricName.SUCCESSFUL_POLL.getValue(), queueName);

        DataPointVO<Long>[] inHour = metricsDataRetriever.getCountsForLastHour(MetricName.ENQUEUE.getValue(), queueName);
        DataPointVO<Long>[] inDay = metricsDataRetriever.getCountsForLastDay(MetricName.ENQUEUE.getValue(), queueName);

        QueueBalanceVO result = new QueueBalanceVO();
        result.setTotalInDay(getTotal(inDay));
        result.setInDayPeriod(getPeriod(inDay));

        result.setTotalInHour(getTotal(inHour));
        result.setInHourPeriod(getPeriod(inHour));

        result.setTotalOutDay(getTotal(outDay));
        result.setOutDayPeriod(getPeriod(outDay));

        result.setTotalOutHour(getTotal(outHour));
        result.setOutHourPeriod(getPeriod(outHour));

        return result;

    }


    private long[] getPeriod(DataPointVO<Long>[] target) {
        long[] result = getInitialValue(target);

        if (target != null && target.length > 0) {
            for (DataPointVO<Long> dp: target) {
                if (dp!=null && dp.getTime()>0) {
                    long time = dp.getTime();
                    if(time<result[0]) {
                        result[0] = time;
                    }
                    if (time>result[1]) {
                        result[1] = time;
                    }
                }
            }
        }

        return result;
    }

    private long[] getInitialValue(DataPointVO <Long>[] target) {
        long[] result = {-1l, -1l};
        if (target != null && target.length > 0) {
            for (DataPointVO<Long> dp: target) {
                if (dp!=null && dp.getTime()>0) {
                    result[0] = dp.getTime();
                    result[1] = dp.getTime();
                    break;
                }
            }
        }
        return result;
    }


    private int getTotal(DataPointVO<Long>[] target) {
        int result = -1;

        if (target != null && target.length > 0) {
            result = 0;
            for (DataPointVO<Long> dp: target) {
                if (dp!=null && dp.getValue()>0) {
                    result += dp.getValue();
                }
            }
        }

        return result;
    }

    public String getQueueName() {
        return queueName;
    }
}

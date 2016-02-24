package ru.taskurotta.service.hz.config;

import ru.taskurotta.service.console.model.MetricsStatDataVO;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.MetricsDataUtils;
import ru.taskurotta.service.metrics.RateUtils;
import ru.taskurotta.service.metrics.handler.MetricsDataHandler;
import ru.taskurotta.service.metrics.model.DataPointVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;

public class AllActorMetricsTask implements Callable<Collection<MetricsStatDataVO>>, Serializable {

    private String actorId;

    public AllActorMetricsTask(String actorId) {
        this.actorId = actorId;
    }

    @Override
    public Collection<MetricsStatDataVO> call() throws Exception {
        MetricsDataHandler metricsDataRetriever = MetricsDataHandler.getInstance();
        if (metricsDataRetriever == null) {
            return new ArrayList<>();
        }

        Collection<MetricsStatDataVO> result = new ArrayList<>();
        for (MetricName metric : MetricName.values()) {
            String metricName = metric.name();
            DataPointVO<Long>[] hourCounts = metricsDataRetriever.getCountsForLastHour(metricName, actorId);
            DataPointVO<Double>[] hourMeans = metricsDataRetriever.getMeansForLastHour(metricName, actorId);

            DataPointVO<Long>[] dayCounts = metricsDataRetriever.getCountsForLastDay(metricName, actorId);
            DataPointVO<Double>[] dayMeans = metricsDataRetriever.getMeansForLastDay(metricName, actorId);

            Date lastActivity = metricsDataRetriever.getLastActivityTime(metricName, actorId);

            MetricsStatDataVO statData = new MetricsStatDataVO();
            statData.setDatasetName(actorId);
            statData.setMetricName(metricName);
            statData.setLastActivity(lastActivity);

            statData.setMeanTimeDay(RateUtils.round(MetricsDataUtils.getMeanTime(dayMeans), 2));
            statData.setMeanTimeHour(RateUtils.round(MetricsDataUtils.getMeanTime(hourMeans), 2));
            statData.setTotalCountsDay(MetricsDataUtils.getTotalCounts(dayCounts));
            statData.setTotalCountsHour(MetricsDataUtils.getTotalCounts(hourCounts));

            result.add(statData);
        }

        return result;
    }
}

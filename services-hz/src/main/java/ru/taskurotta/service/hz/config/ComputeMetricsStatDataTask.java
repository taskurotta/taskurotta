package ru.taskurotta.service.hz.config;

import ru.taskurotta.service.console.model.MetricsStatDataVO;
import ru.taskurotta.service.statistics.MetricsDataHandler;
import ru.taskurotta.service.statistics.metrics.MetricsDataUtils;
import ru.taskurotta.service.statistics.metrics.RateUtils;
import ru.taskurotta.service.statistics.metrics.data.DataPointVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * User: dimadin
 * Date: 08.10.13 17:02
 */
public class ComputeMetricsStatDataTask implements Callable<Collection<MetricsStatDataVO>>, Serializable {

    private Collection<String> metricsNames;
    private Collection<String> actorIds;

    public ComputeMetricsStatDataTask(Collection<String> metricsNames, Collection<String> actorIds) {
        this.metricsNames = metricsNames;
        this.actorIds = actorIds;
    }

    @Override
    public Collection<MetricsStatDataVO> call() throws Exception {

        MetricsDataHandler metricsDataRetriever = MetricsDataHandler.getInstance();
        if (metricsDataRetriever == null) {
            return null;
        }

        Collection<MetricsStatDataVO> result = new ArrayList<>();

        for (String actorId: actorIds) {
            for (String metricName: metricsNames) {
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
        }

        return result;
    }

}

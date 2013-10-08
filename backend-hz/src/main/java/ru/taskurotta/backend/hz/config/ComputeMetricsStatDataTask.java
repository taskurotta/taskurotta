package ru.taskurotta.backend.hz.config;

import ru.taskurotta.backend.console.model.MetricsStatDataVO;
import ru.taskurotta.backend.console.retriever.MetricsDataRetriever;
import ru.taskurotta.backend.statistics.DataPointVO;
import ru.taskurotta.backend.statistics.MetricsDataHandler;
import ru.taskurotta.backend.statistics.metrics.MetricsDataUtils;
import ru.taskurotta.backend.statistics.metrics.RateUtils;

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

        MetricsDataRetriever metricsDataRetriever = MetricsDataHandler.getDataRetrieverInstance();
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

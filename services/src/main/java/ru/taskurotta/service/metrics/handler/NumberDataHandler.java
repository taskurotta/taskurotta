package ru.taskurotta.service.metrics.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.retriever.metrics.MetricsNumberDataRetriever;
import ru.taskurotta.service.metrics.MetricsDataUtils;
import ru.taskurotta.service.metrics.model.DataPointVO;
import ru.taskurotta.service.metrics.model.NumberDataRowVO;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for metrics with simple numeric data. Stores and exposes data.
 * User: dimadin
 * Date: 25.10.13 10:30
 */
public class NumberDataHandler implements NumberDataListener, MetricsNumberDataRetriever {

    private static NumberDataHandler singleton;
    private Map<String, NumberDataRowVO> dataHolder = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(NumberDataHandler.class);


    public static NumberDataHandler getInstance() {
        return singleton;
    }

    @PostConstruct
    public void init() {
        singleton = this;
    }

    @Override
    public void handleNumberData(String metricName, String datasetName, Number value, long currentTime, int maxPoints) {
        String holderKey = MetricsDataUtils.getKey(metricName, datasetName);

        NumberDataRowVO dataRow = dataHolder.get(holderKey);
        if(dataRow == null) {
            synchronized (dataHolder) {
                dataRow = dataHolder.get(holderKey);
                if(dataRow == null) {
                    dataRow = new NumberDataRowVO(maxPoints, metricName, datasetName);
                    dataHolder.put(holderKey, dataRow);
                }
            }
        }

        int position = dataRow.populate(value, currentTime);

        logger.debug("Handled [{}] data for position [{}], metric[{}], dataset[{}], value[{}], measurementTime[{}]", value.getClass(), position, metricName, datasetName, value, currentTime);

    }

    @Override
    public Collection<String> getNumberMetricNames() {
        Set<String> result = new HashSet<>();
        for (NumberDataRowVO value: dataHolder.values()) {
            result.add(value.getMetricsName());
        }
        return result;
    }

    @Override
    public Collection<String> getNumberDataSets(String metricName) {
        Set<String> result = new HashSet<>();
        for (NumberDataRowVO value: dataHolder.values()) {
            if (value.getMetricsName().equals(metricName)) {
                result.add(value.getDataSetName());
            }
        }
        return result;
    }

    @Override
    public DataPointVO<Number>[] getData(String metricName, String datasetName) {
        DataPointVO<Number>[] result = null;
        NumberDataRowVO value = dataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            result = value.getCurrentData();
        }

        return result;
    }

    @Override
    public Date getLastActivityTime(String metricName, String datasetName) {
        Date result = null;
        NumberDataRowVO value = dataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            result = new Date(value.getLatestActivity());
        }
        return result;
    }

    @Override
    public Number getLastValue(String metricName, String datasetName) {
        Number result = null;
        NumberDataRowVO value = dataHolder.get(MetricsDataUtils.getKey(metricName, datasetName));
        if (value != null) {
            DataPointVO<Number>[] data = value.getCurrentData();
            if (data!=null && data.length>0) {
                MetricsDataUtils.sortDataSet(data);
                result = data[data.length-1].getValue();
            }
        }
        logger.debug("Last value for metric[{}], dataset[{}] is [{}]", metricName, datasetName, result);
        return result;
    }
}

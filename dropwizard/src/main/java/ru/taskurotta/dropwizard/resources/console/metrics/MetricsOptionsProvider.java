package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.backend.console.retriever.metrics.MetricsNumberDataRetriever;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConsoleUtils;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConstants;
import ru.taskurotta.dropwizard.resources.console.metrics.support.OptionsSupportBean;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.AvailableOptionsVO;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.OptionVO;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides information about available metrics options for console
 * User: dimadin
 * Date: 09.09.13 16:34
 */
public class MetricsOptionsProvider implements MetricsConstants {

    private static final Logger logger = LoggerFactory.getLogger(MetricsOptionsProvider.class);

    private MetricsMethodDataRetriever methodDataRetriever;
    private MetricsNumberDataRetriever numberDataRetriever;
    private OptionsSupportBean optionsSupportBean;

    public AvailableOptionsVO getAvailableOptions() {
        Collection<String> methodMetricNames = methodDataRetriever.getMetricNames();
        Collection<String> numberMetricsNames = numberDataRetriever.getNumberMetricNames();

        AvailableOptionsVO result = new AvailableOptionsVO();

        List<OptionVO> metrics = optionsSupportBean.getFullMetricsNameList(numberMetricsNames, methodMetricNames);
        result.setMetrics(metrics);

        Map<String, List<OptionVO>> datasets = optionsSupportBean.getFullDatasetList(getNumberDatasetOptions(numberMetricsNames), getMethodDatasetOptions(methodMetricNames));
        result.setDataSets(datasets);

        result.setDataTypes(optionsSupportBean.getAvailableDataTypes(metrics, true));
        result.setPeriods(optionsSupportBean.getAvailablePeriods(metrics, true));
        result.setScopes(optionsSupportBean.getAvailableScopes(metrics, true));

        logger.debug("Available metrics options found are [{}]", result);
        return result;
    }

    public Map<String, List<OptionVO>> getMethodDatasetOptions(Collection<String> metrics) {
        Map<String, List<OptionVO>> result = null;

        if (metrics!=null && !metrics.isEmpty()) {
            result = new HashMap();
            for (String metric : metrics) {
                Collection<String> dataSetNames = methodDataRetriever.getDataSetsNames(metric);
                List<OptionVO> dataSetDescs = MetricsConsoleUtils.setIsGeneralFlag(MetricsConsoleUtils.getAsOptions(dataSetNames), metric);
                result.put(metric, dataSetDescs);
            }
        }

        return result;
    }

    public Map<String, List<OptionVO>> getNumberDatasetOptions(Collection<String> metrics) {
        Map<String, List<OptionVO>> result = null;

        if (metrics!=null && !metrics.isEmpty()) {
            result = new HashMap();
            for (String metric : metrics) {
                Collection<String> dataSetNames = numberDataRetriever.getNumberDataSets(metric);
                List<OptionVO> dataSetDescs = MetricsConsoleUtils.setIsGeneralFlag(MetricsConsoleUtils.getAsOptions(dataSetNames), metric);
                result.put(metric, dataSetDescs);
            }
        }

        return result;
    }

    @Required
    public void setMethodDataRetriever(MetricsMethodDataRetriever dataRetriever) {
        this.methodDataRetriever = dataRetriever;
    }

    @Required
    public void setNumberDataRetriever(MetricsNumberDataRetriever numberDataRetriever) {
        this.numberDataRetriever = numberDataRetriever;
    }

    @Required
    public void setOptionsSupportBean(OptionsSupportBean optionsSupportBean) {
        this.optionsSupportBean = optionsSupportBean;
    }
}

package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConsoleUtils;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConstants;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.MetricsOptionsVO;
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

    private MetricsMethodDataRetriever dataRetriever;

    private Map<String, String> dataTypes;
    private Map<String, String> scopes;
    private Map<String, String> periods;

    public MetricsOptionsVO getMethodMetricsTypes() {
        MetricsOptionsVO result = new MetricsOptionsVO();
        List<OptionVO> metricsDesc = MetricsConsoleUtils.getAsOptions(dataRetriever.getMetricNames());
        result.setMetricDesc(metricsDesc);
        result.setDataSetDesc(getMethodDataSetDescOptions(metricsDesc));
        result.setDataTypes( MetricsConsoleUtils.getOptionsFromMap(dataTypes));
        result.setPeriods(MetricsConsoleUtils.getOptionsFromMap(periods));
        result.setScopes(MetricsConsoleUtils.getOptionsFromMap(scopes));

        logger.debug("Metrics options getted [{}]", result);

        return result;
    }

    public Map<String, List<OptionVO>> getMethodDataSetDescOptions(List<OptionVO> metrics) {
        Map<String, List<OptionVO>> result = null;

        if (metrics!=null && !metrics.isEmpty()) {
            result = new HashMap();
            for (OptionVO metric : metrics) {
                Collection<String> dataSetNames = dataRetriever.getDataSetsNames(metric.getValue());
                List<OptionVO> dataSetDescs = MetricsConsoleUtils.setIsGeneralFlag(MetricsConsoleUtils.getAsOptions(dataSetNames), metric.getValue());
                result.put(metric.getValue(), dataSetDescs);
            }
        }

        return result;
    }

    @Required
    public void setDataTypes(Map<String, String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    @Required
    public void setScopes(Map<String, String> scopes) {
        this.scopes = scopes;
    }

    @Required
    public void setPeriods(Map<String, String> periods) {
        this.periods = periods;
    }

    @Required
    public void setDataRetriever(MetricsMethodDataRetriever dataRetriever) {
        this.dataRetriever = dataRetriever;
    }
}

package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.console.retriever.MetricsDataRetriever;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides information about available metrics options for console
 * User: dimadin
 * Date: 09.09.13 16:34
 */
public class MetricsOptionsHandler implements MetricsConstants {

    private static final Logger logger = LoggerFactory.getLogger(MetricsOptionsHandler.class);

    private MetricsDataRetriever dataRetriever;

    private Map<String, String> dataTypes;
    private Map<String, String> scopes;
    private Map<String, String> periods;

    public Response getMetricsTypes() {
        MetricsOptionsVO result = new MetricsOptionsVO();
        List<OptionVO> metricsDesc = getAsOptions(dataRetriever.getMetricNames());
        result.metricDesc = metricsDesc;
        result.dataSetDesc = getDataSetDescOptions(metricsDesc);
        result.dataTypes = getOptionsFromMap(dataTypes);
        result.periods = getOptionsFromMap(periods);
        result.scopes = getOptionsFromMap(scopes);

        logger.debug("Metrics options getted [{}]", result);

        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }


    public Map<String, List<OptionVO>> getDataSetDescOptions(List<OptionVO> metrics) {
        Map<String, List<OptionVO>> result = null;

        if (metrics!=null && !metrics.isEmpty()) {
            result = new HashMap();
            for (OptionVO metric : metrics) {
                Collection<String> dataSetNames = dataRetriever.getDataSetsNames(metric.getValue());
                List<OptionVO> dataSetDescs = setIsGeneralFlag(getAsOptions(dataSetNames), metric.getValue());
                result.put(metric.getValue(), dataSetDescs);
            }
        }

        return result;
    }

    private List<OptionVO> setIsGeneralFlag(List<OptionVO> target, String dataSetValueToMark) {
        if(target!=null && !target.isEmpty()) {
            for(OptionVO option: target) {
                if(dataSetValueToMark.equals(option.getValue())) {
                    option.setGeneral(true);
                }
            }
        }
        return target;
    }

    private List<OptionVO> getAsOptions(Collection<String> metricsNames) {
        List<OptionVO> result = new ArrayList<>();
        int i = 0;

        if (metricsNames != null && !metricsNames.isEmpty()) {
            for(String metricsName : metricsNames) {
                OptionVO item = new OptionVO();
                item.setName(metricsName);
                item.setValue(metricsName);
                result.add(item);
            }
        }

        return result;
    }

    private List<OptionVO> getOptionsFromMap(Map<String, String> target) {
        List<OptionVO> result = new ArrayList<>();
        if (target != null && !target.isEmpty()) {
            for (String key: target.keySet()) {
                OptionVO option= new OptionVO();
                option.setName(target.get(key));
                option.setValue(key);
                result.add(option);
            }
        }
        return result;
    }

    protected static class MetricsOptionsVO implements Serializable {

        protected List<OptionVO> dataTypes;////metrics dataTypes: ex., rate or execution time
        protected List<OptionVO> scopes;//metrics scope: ex., local node or cluster
        protected List<OptionVO> periods;//metrics intervals: ex., last hour or last 24 hours
        protected List<OptionVO> metricDesc;
        protected Map<String, List<OptionVO>> dataSetDesc;

        public List<OptionVO> getDataTypes() {
            return dataTypes;
        }

        public List<OptionVO> getScopes() {
            return scopes;
        }

        public List<OptionVO> getPeriods() {
            return periods;
        }

        public List<OptionVO> getMetricDesc() {
            return metricDesc;
        }

        public Map<String, List<OptionVO>> getDataSetDesc() {
            return dataSetDesc;
        }

        @Override
        public String toString() {
            return "MetricsOptionsVO{" +
                    "dataTypes=" + dataTypes +
                    ", scopes=" + scopes +
                    ", periods=" + periods +
                    ", metricDesc=" + metricDesc +
                    ", dataSetDesc=" + dataSetDesc +
                    "} ";
        }
    }

    /**
     * Metric option POJO object
     */
    protected static class OptionVO implements Serializable {

        private String value;
        private String name;
        private boolean general = false;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public boolean isGeneral() {
            return general;
        }

        public void setGeneral(boolean general) {
            this.general = general;
        }

        @Override
        public String toString() {
            return "OptionVO{" +
                    "value='" + value + '\'' +
                    ", name='" + name + '\'' +
                    ", general=" + general +
                    "} ";
        }
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
    public void setDataRetriever(MetricsDataRetriever dataRetriever) {
        this.dataRetriever = dataRetriever;
    }
}

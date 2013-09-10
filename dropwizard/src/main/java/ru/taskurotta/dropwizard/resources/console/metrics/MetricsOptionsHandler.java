package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides information about available metrics options for console
 * User: dimadin
 * Date: 09.09.13 16:34
 */
public class MetricsOptionsHandler implements MetricsConstants {

    private static final Logger logger = LoggerFactory.getLogger(MetricsOptionsHandler.class);

    private Map<String, String> dataTypes;
    private Map<String, String> scopes;
    private Map<String, String> periods;
    private Map<String, String> metrics;

    public Response getMetricsTypes() {
        MetricsOptionsVO result = new MetricsOptionsVO();
        result.dataTypes = getOptionsFromMap(dataTypes);
        result.metrics = getOptionsFromMap(metrics);
        result.periods = getOptionsFromMap(periods);
        result.scopes = getOptionsFromMap(scopes);

        return Response.ok(result, MediaType.APPLICATION_JSON).build();
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
        protected List<OptionVO> dataTypes;
        protected List<OptionVO> scopes;
        protected List<OptionVO> periods;
        protected List<OptionVO> metrics;

        public List<OptionVO> getDataTypes() {
            return dataTypes;
        }

        public List<OptionVO> getScopes() {
            return scopes;
        }

        public List<OptionVO> getPeriods() {
            return periods;
        }

        public List<OptionVO> getMetrics() {
            return metrics;
        }

        @Override
        public String toString() {
            return "MetricsOptionVO{" +
                    "dataTypes=" + dataTypes +
                    ", scopes=" + scopes +
                    ", periods=" + periods +
                    ", metrics=" + metrics +
                    "} ";
        }
    }

    protected static class OptionVO implements Serializable {

        private String value;
        private String name;

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

        @Override
        public String toString() {
            return "OptionVO{" +
                    "value='" + value + '\'' +
                    ", name='" + name + '\'' +
                    "} ";
        }
    }

    public void setDataTypes(Map<String, String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public void setScopes(Map<String, String> scopes) {
        this.scopes = scopes;
    }

    public void setPeriods(Map<String, String> periods) {
        this.periods = periods;
    }

    public void setMetrics(Map<String, String> metrics) {
        this.metrics = metrics;
    }
}

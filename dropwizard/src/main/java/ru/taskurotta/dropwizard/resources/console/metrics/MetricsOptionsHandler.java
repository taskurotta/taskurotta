package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.statistics.ActorMetricsManager;
import ru.taskurotta.backend.statistics.GeneralMetricsManager;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides information about available metrics options for console
 * User: dimadin
 * Date: 09.09.13 16:34
 */
public class MetricsOptionsHandler implements MetricsConstants {

    private static final Logger logger = LoggerFactory.getLogger(MetricsOptionsHandler.class);

    private ActorMetricsManager actorMetricsManager;
    private GeneralMetricsManager generalMetricsManager;

    private Map<String, String> dataTypes;
    private Map<String, String> scopes;
    private Map<String, String> periods;

    public Response getMetricsTypes() {
        Collection<String> actorMetrics = actorMetricsManager.getNames();
        Collection<String> generalMetrics = generalMetricsManager.getNames();

        logger.debug("ActorMetrics getted [{}], generalMetrics getted [{}]", actorMetrics, generalMetrics);

        MetricsOptionsVO result = new MetricsOptionsVO();
        result.actorMetrics = getMetricsTypeOptions(actorMetrics);
        result.generalMetrics = getMetricsTypeOptions(generalMetrics);
        result.dataTypes = getOptionsFromMap(dataTypes);
        result.periods = getOptionsFromMap(periods);
        result.scopes = getOptionsFromMap(scopes);

        return Response.ok(result, MediaType.APPLICATION_JSON).build();
    }


    private List<OptionVO> getMetricsTypeOptions(Collection<String> metricsNames) {
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
        protected List<OptionVO> generalMetrics;
        protected List<OptionVO> actorMetrics;


        public List<OptionVO> getDataTypes() {
            return dataTypes;
        }

        public List<OptionVO> getScopes() {
            return scopes;
        }

        public List<OptionVO> getPeriods() {
            return periods;
        }

        public List<OptionVO> getGeneralMetrics() {
            return generalMetrics;
        }

        public List<OptionVO> getActorMetrics() {
            return actorMetrics;
        }

        @Override
        public String toString() {
            return "MetricsOptionsVO{" +
                    "dataTypes=" + dataTypes +
                    ", scopes=" + scopes +
                    ", periods=" + periods +
                    ", generalMetrics=" + generalMetrics +
                    ", actorMetrics=" + actorMetrics +
                    "} ";
        }
    }

    /**
     * Metric option POJO object
     */
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
    public void setActorMetricsManager(ActorMetricsManager actorMetricsManager) {
        this.actorMetricsManager = actorMetricsManager;
    }

    @Required
    public void setGeneralMetricsManager(GeneralMetricsManager generalMetricsManager) {
        this.generalMetricsManager = generalMetricsManager;
    }
}

package ru.taskurotta.dropwizard.resources.console.metrics.support;

import ru.taskurotta.backend.statistics.metrics.MetricsDataUtils;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.OptionVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides support information on metrics options, like human readable descriptions
 * User: dimadin
 * Date: 25.10.13 12:23
 */
public class OptionsSupportBean {

    private Map<String, String> metricsDescription;
    private Map<String, String> datasetDescription;

    private Map<String, String> metricDataTypes;
    private Map<String, String> metricScopes;
    private Map<String, String> metricPeriods;

    private String defaultDataTypes;
    private String defaultScopes;
    private String defaultPeriods;

    private Map<String, String> optionDescriptions;

    public Map<String, List<OptionVO>> getAvailableDataTypes(List<OptionVO> metrics, boolean defaultsFallBack) {
        return getAvailableTypes(metrics, defaultsFallBack, metricDataTypes, defaultDataTypes);
    }

    public Map<String, List<OptionVO>> getAvailableScopes(List<OptionVO> metrics, boolean defaultsFallBack) {
        return getAvailableTypes(metrics, defaultsFallBack, metricScopes, defaultScopes);
    }

    public Map<String, List<OptionVO>> getAvailablePeriods(List<OptionVO> metrics, boolean defaultsFallBack) {
        return getAvailableTypes(metrics, defaultsFallBack, metricPeriods, defaultPeriods);
    }

    private Map<String, List<OptionVO>> getAvailableTypes(List<OptionVO> metrics, boolean defaultsFallBack, Map<String, String> types, String defaultTypes) {
        Map<String, List<OptionVO>> result = null;
        if (metrics!=null && !metrics.isEmpty()) {
            result = new HashMap<>();
            for (OptionVO metric: metrics) {
                List<String> typesList = null;
                if (types.containsKey(metric.getValue())) {
                    typesList = getCommaSeparatedAsList(types.get(metric.getValue()));

                } else if (defaultsFallBack) {
                    typesList = getCommaSeparatedAsList(defaultTypes);
                }
                result.put(metric.getValue(), getAsOptions(typesList));
            }
        }
        return result;
    }

    public List<OptionVO> getFullMetricsNameList(Collection<String>... mergeable) {
        List<OptionVO> result = new ArrayList<>();

        for (Collection<String> metricNames : mergeable) {
            if(metricNames!=null && !metricNames.isEmpty()) {
                for (String metric: metricNames) {
                    OptionVO option = new OptionVO();
                    option.setValue(metric);
                    option.setName(getMetricDescription(metric));
                    result.add(option);
                }
            }
        }

        return result;
    }

    public Map<String, List<OptionVO>> getFullDatasetList(Map<String, List<OptionVO>>... mergeable) {
        Map<String, List<OptionVO>> result = new HashMap<>();

        for (Map<String, List<OptionVO>> datasets : mergeable) {
            if(datasets!=null && !datasets.isEmpty()) {
                for (String metricName: datasets.keySet()) {
                    List<OptionVO> metricDatasets = datasets.get(metricName);
                    injectDatasetDescriptionIfAny(metricName, metricDatasets);
                    result.put(metricName, metricDatasets);
                }
            }
        }

        return result;
    }

    private List<String> getCommaSeparatedAsList(String target) {
        List<String> result = null;
        if (target!=null && target.trim().length()>0) {
            result = Arrays.asList(target.split(",\\s*"));
        }
        return result;
    }

    private String getMetricDescription(String metricName) {
        String result = metricName;
        if (metricsDescription!=null && metricsDescription.containsKey(metricName)) {
            result = metricsDescription.get(metricName);
        }
        return result;
    }

    private void injectDatasetDescriptionIfAny(String metricName, List<OptionVO> target) {
        if (target!=null && !target.isEmpty()) {
            for (OptionVO option: target) {
                option.setName(getDatasetDescription(metricName, option.getValue()));
            }
        }
    }

    private String getDatasetDescription (String metricName, String datasetName) {
        String result = null;
        if (datasetDescription!=null) {
            result = datasetDescription.get(MetricsDataUtils.getKey(metricName, datasetName));
        }
        return result!=null? result : datasetName;
    }

    private List<OptionVO> getAsOptions(List<String> types) {
        List<OptionVO> result = null;
        if (types!=null && !types.isEmpty()) {
            result = new ArrayList<>();
            for (String type: types) {
                OptionVO option = new OptionVO();
                option.setValue(type);
                if (optionDescriptions!=null) {
                    option.setName(optionDescriptions.get(type));
                }
                if (option.getName() == null) {
                    option.setName(type);
                }
                result.add(option);
            }
        }
        return result;
    }

    public void setMetricsDescription(Map<String, String> metricsDescription) {
        this.metricsDescription = metricsDescription;
    }

    public void setDatasetDescription(Map<String, String> datasetDescription) {
        this.datasetDescription = datasetDescription;
    }

    public void setMetricDataTypes(Map<String, String> metricDataTypes) {
        this.metricDataTypes = metricDataTypes;
    }

    public void setMetricScopes(Map<String, String> metricScopes) {
        this.metricScopes = metricScopes;
    }

    public void setMetricPeriods(Map<String, String> metricPeriods) {
        this.metricPeriods = metricPeriods;
    }

    public void setDefaultDataTypes(String defaultDataTypes) {
        this.defaultDataTypes = defaultDataTypes;
    }

    public void setDefaultScopes(String defaultScopes) {
        this.defaultScopes = defaultScopes;
    }

    public void setDefaultPeriods(String defaultPeriods) {
        this.defaultPeriods = defaultPeriods;
    }

    public void setOptionDescriptions(Map<String, String> optionDescriptions) {
        this.optionDescriptions = optionDescriptions;
    }
}

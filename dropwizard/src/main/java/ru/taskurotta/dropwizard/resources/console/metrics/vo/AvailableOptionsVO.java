package ru.taskurotta.dropwizard.resources.console.metrics.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Object representing available options for displaying metrics data.
 * Different metrics can have different data types, periods, scopes and so on
 * User: dimadin
 * Date: 25.10.13 12:18
 */
public class AvailableOptionsVO implements Serializable {

    protected List<OptionVO> metrics;
    protected Map<String, List<OptionVO>> dataSets;//List of available datasets for the metric
    protected Map<String, List<OptionVO>> dataTypes;//List of available dataTypes for the metric
    protected Map<String, List<OptionVO>> scopes;//List of available scopes for the metric
    protected Map<String, List<OptionVO>> periods;//List of available period for the metric

    public List<OptionVO> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<OptionVO> metrics) {
        this.metrics = metrics;
    }

    public Map<String, List<OptionVO>> getDataSets() {
        return dataSets;
    }

    public void setDataSets(Map<String, List<OptionVO>> dataSets) {
        this.dataSets = dataSets;
    }

    public Map<String, List<OptionVO>> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(Map<String, List<OptionVO>> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public Map<String, List<OptionVO>> getScopes() {
        return scopes;
    }

    public void setScopes(Map<String, List<OptionVO>> scopes) {
        this.scopes = scopes;
    }

    public Map<String, List<OptionVO>> getPeriods() {
        return periods;
    }

    public void setPeriods(Map<String, List<OptionVO>> periods) {
        this.periods = periods;
    }

    @Override
    public String toString() {
        return "AvailableOptionsVO{" +
                "metrics=" + metrics +
                ", dataSets=" + dataSets +
                ", dataTypes=" + dataTypes +
                ", scopes=" + scopes +
                ", periods=" + periods +
                "} ";
    }

}

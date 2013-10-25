package ru.taskurotta.dropwizard.resources.console.metrics.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 25.10.13 11:55
 */
public class MetricsOptionsVO implements Serializable {

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

    public void setDataTypes(List<OptionVO> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public void setScopes(List<OptionVO> scopes) {
        this.scopes = scopes;
    }

    public void setPeriods(List<OptionVO> periods) {
        this.periods = periods;
    }

    public void setMetricDesc(List<OptionVO> metricDesc) {
        this.metricDesc = metricDesc;
    }

    public void setDataSetDesc(Map<String, List<OptionVO>> dataSetDesc) {
        this.dataSetDesc = dataSetDesc;
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

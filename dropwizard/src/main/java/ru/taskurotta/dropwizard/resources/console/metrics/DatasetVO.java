package ru.taskurotta.dropwizard.resources.console.metrics;

import java.io.Serializable;
import java.util.List;

/**
 * Data object representing set of metrics data points
 * User: dimadin
 * Date: 05.09.13 16:27
 */
public class DatasetVO implements Serializable {
    private int id = 0;
    private List<Number[]> data;
    private String label;
    private String xLabel;
    private String yLabel;

    public List<Number[]> getData() {
        return data;
    }

    public void setData(List<Number[]> data) {
        this.data = data;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getxLabel() {
        return xLabel;
    }

    public void setxLabel(String xLabel) {
        this.xLabel = xLabel;
    }

    public String getyLabel() {
        return yLabel;
    }

    public void setyLabel(String yLabel) {
        this.yLabel = yLabel;
    }

    @Override
    public String toString() {
        return "DatasetVO{" +
                "id=" + id +
                ", data=" + data +
                ", label='" + label + '\'' +
                ", xLabel='" + xLabel + '\'' +
                ", yLabel='" + yLabel + '\'' +
                "} ";
    }

}

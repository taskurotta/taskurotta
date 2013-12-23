package ru.taskurotta.dropwizard.resources.console.metrics.vo;

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

    private String yFormatter;
    private String xFormatter;
    private int xTicks;
    private int yTicks;

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

    public String getyFormatter() {
        return yFormatter;
    }

    public void setyFormatter(String yFormatter) {
        this.yFormatter = yFormatter;
    }

    public String getxFormatter() {
        return xFormatter;
    }

    public void setxFormatter(String xFormatter) {
        this.xFormatter = xFormatter;
    }

    public int getxTicks() {
        return xTicks;
    }

    public void setxTicks(int xTicks) {
        this.xTicks = xTicks;
    }

    public int getyTicks() {
        return yTicks;
    }

    public void setyTicks(int yTicks) {
        this.yTicks = yTicks;
    }

    @Override
    public String toString() {
        return "DatasetVO{" +
                "id=" + id +
                ", data=" + data +
                ", label='" + label + '\'' +
                ", xLabel='" + xLabel + '\'' +
                ", yLabel='" + yLabel + '\'' +
                ", yFormatter='" + yFormatter + '\'' +
                ", xFormatter='" + xFormatter + '\'' +
                ", xTicks='" + xTicks + '\'' +
                ", yTicks='" + yTicks + '\'' +
                "} ";
    }

}

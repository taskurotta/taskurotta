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

    private boolean clickable = true;
    private boolean hoverable = true;

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

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean isHoverable() {
        return hoverable;
    }

    public void setHoverable(boolean hoverable) {
        this.hoverable = hoverable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "DatasetVO{" +
                "id=" + id +
                ", data=" + data +
                ", label='" + label + '\'' +
                ", clickable=" + clickable +
                ", hoverable=" + hoverable +
                "} " + super.toString();
    }

}

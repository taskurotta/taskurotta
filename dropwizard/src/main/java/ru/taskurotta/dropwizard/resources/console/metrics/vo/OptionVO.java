package ru.taskurotta.dropwizard.resources.console.metrics.vo;

import java.io.Serializable;

/**
 * Metric option POJO object. Metrics name and human readable description
 * User: dimadin
 * Date: 25.10.13 11:54
 */
public class OptionVO implements Serializable {

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

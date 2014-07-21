package ru.taskurotta.dropwizard.resources.console.schedule.model;

import java.io.Serializable;

/**
 * Created on 17.07.2014.
 */
public class ArgVO implements Serializable {
    private String type;//TODO: introduce enum type?
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArgVO that = (ArgVO) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ArgumentVO{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

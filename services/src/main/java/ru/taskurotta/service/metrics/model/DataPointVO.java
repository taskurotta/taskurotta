package ru.taskurotta.service.metrics.model;

/**
 * POJO wrapper for metrics data point.
 * User: dimadin
 * Date: 15.09.13 16:28
 */
public class DataPointVO<T extends Number> {
    private long time;
    private T value;

    public DataPointVO(T value, long time) {
        this.value = value;
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void update(T value, long time) {
        this.value = value;
        this.time = time;
    }

    @Override
    public String toString() {
        return "DataPointVO{" +
                "time=" + time +
                ", value=" + value +
                "} ";
    }

}

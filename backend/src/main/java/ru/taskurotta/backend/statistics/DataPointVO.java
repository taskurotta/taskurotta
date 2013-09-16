package ru.taskurotta.backend.statistics;

/**
 * POJO wrapper for metrics data point.
 * User: dimadin
 * Date: 15.09.13 16:28
 */
public class DataPointVO<T> {
    private long time;
    private T value;

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

    @Override
    public String toString() {
        return "DataPointVO{" +
                "time=" + time +
                ", value=" + value +
                "} ";
    }
}

package ru.taskurotta.backend.console.model;

/**
 * POJO representation of a method profile
 * User: dimadin
 * Date: 28.05.13 14:23
 */
public class ProfileVO {

    private String name;
    private long max = 0;
    private long min = 0;
    private double mean = 0;
    private long measured = 0;

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public long getMeasured() {
        return measured;
    }

    public void setMeasured(long measured) {
        this.measured = measured;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ProfileVO{" +
                "name='" + name + '\'' +
                ", max=" + max +
                ", min=" + min +
                ", mean=" + mean +
                ", measured=" + measured +
                '}';
    }

}

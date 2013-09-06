package ru.taskurotta.dropwizard.resources.console.metrics;

/**
 * POJO representing type of supported metric
 * User: dimadin
 * Date: 06.09.13 15:32
 */
public class MetricTypeVO {

    private int type;
    private String name;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MetricTypeVO {" +
                "type=" + type +
                ", name='" + name + '\'' +
                "} ";
    }

}

package ru.taskurotta.backend.statistics;

import java.io.Serializable;

/**
 * POJO representing activity of the queue.
 * Queue incomes and outcomes measured by metrics
 * User: dimadin
 * Date: 01.10.13 11:15
 */
public class QueueStateVO implements Serializable {

    private int inHour = -1;
    private int outHour = -1;
    private int inDay = -1;
    private int outDay = -1;

    public int getInHour() {
        return inHour;
    }

    public void setInHour(int inHour) {
        this.inHour = inHour;
    }

    public int getOutHour() {
        return outHour;
    }

    public void setOutHour(int outHour) {
        this.outHour = outHour;
    }

    public int getInDay() {
        return inDay;
    }

    public void setInDay(int inDay) {
        this.inDay = inDay;
    }

    public int getOutDay() {
        return outDay;
    }

    public void setOutDay(int outDay) {
        this.outDay = outDay;
    }

    @Override
    public String toString() {
        return "QueueStateVO{" +
                "inHour=" + inHour +
                ", outHour=" + outHour +
                ", inDay=" + inDay +
                ", outDay=" + outDay +
                "} ";
    }
}

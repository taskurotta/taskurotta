package ru.taskurotta.service.metrics.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * POJO representing activity of the queue.
 * Queue incomes and outcomes measured by metrics
 * User: dimadin
 * Date: 01.10.13 11:15
 */
public class QueueBalanceVO implements Serializable {

    private int totalInHour = -1;
    private long[] inHourPeriod = {-1l, -1l};

    private int totalOutHour = -1;
    private long[] outHourPeriod = {-1l, -1l};

    private int totalInDay = -1;
    private long[] inDayPeriod = {-1l, -1l};

    private int totalOutDay = -1;
    private long[] outDayPeriod = {-1l, -1l};

    private int nodes = 0;

    public int getTotalInHour() {
        return totalInHour;
    }

    public void setTotalInHour(int totalInHour) {
        this.totalInHour = totalInHour;
    }

    public long[] getInHourPeriod() {
        return inHourPeriod;
    }

    public void setInHourPeriod(long[] inHourPeriod) {
        this.inHourPeriod = inHourPeriod;
    }

    public int getTotalOutHour() {
        return totalOutHour;
    }

    public void setTotalOutHour(int totalOutHour) {
        this.totalOutHour = totalOutHour;
    }

    public long[] getOutHourPeriod() {
        return outHourPeriod;
    }

    public void setOutHourPeriod(long[] outHourPeriod) {
        this.outHourPeriod = outHourPeriod;
    }

    public int getTotalInDay() {
        return totalInDay;
    }

    public void setTotalInDay(int totalInDay) {
        this.totalInDay = totalInDay;
    }

    public long[] getInDayPeriod() {
        return inDayPeriod;
    }

    public void setInDayPeriod(long[] inDayPeriod) {
        this.inDayPeriod = inDayPeriod;
    }

    public int getTotalOutDay() {
        return totalOutDay;
    }

    public void setTotalOutDay(int totalOutDay) {
        this.totalOutDay = totalOutDay;
    }

    public long[] getOutDayPeriod() {
        return outDayPeriod;
    }

    public void setOutDayPeriod(long[] outDayPeriod) {
        this.outDayPeriod = outDayPeriod;
    }

    public int getNodes() {
        return nodes;
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "QueueBalanceVO{" +
                "totalInHour=" + totalInHour +
                ", inHourPeriod=" + Arrays.toString(inHourPeriod) +
                ", totalOutHour=" + totalOutHour +
                ", outHourPeriod=" + Arrays.toString(outHourPeriod) +
                ", totalInDay=" + totalInDay +
                ", inDayPeriod=" + Arrays.toString(inDayPeriod) +
                ", totalOutDay=" + totalOutDay +
                ", outDayPeriod=" + Arrays.toString(outDayPeriod) +
                ", nodes=" + nodes +
                "} ";
    }
}

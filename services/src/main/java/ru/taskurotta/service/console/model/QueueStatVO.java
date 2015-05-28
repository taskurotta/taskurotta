package ru.taskurotta.service.console.model;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO representing queue statistics data
 * User: dimadin
 * Date: 29.11.13 12:41
 */
public class QueueStatVO implements Serializable {

    private String name;
    private int count = 0;
    private Date lastActivity = null;
    private long lastPolledTaskEnqueueTime = -1l;

    private long inHour = 0;
    private long outHour = 0;

    private long inDay = 0;
    private long outDay = 0;

    private int nodes = 0;

    private boolean local = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }

    public long getInHour() {
        return inHour;
    }

    public void setInHour(long inHour) {
        this.inHour = inHour;
    }

    public long getOutHour() {
        return outHour;
    }

    public void setOutHour(long outHour) {
        this.outHour = outHour;
    }

    public long getInDay() {
        return inDay;
    }

    public void setInDay(long inDay) {
        this.inDay = inDay;
    }

    public long getOutDay() {
        return outDay;
    }

    public void setOutDay(long outDay) {
        this.outDay = outDay;
    }

    public int getNodes() {
        return nodes;
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    public long getLastPolledTaskEnqueueTime() {
        return lastPolledTaskEnqueueTime;
    }

    public void setLastPolledTaskEnqueueTime(long lastPolledTaskEnqueueTime) {
        this.lastPolledTaskEnqueueTime = lastPolledTaskEnqueueTime;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public QueueStatVO sumValuesWith(QueueStatVO qs) {
        if (qs != null) {

            this.count = qs.getCount();//counts are same on every node, so the last measured value should be correct

            if (this.lastActivity==null
                    || (qs.getLastActivity()!=null && qs.getLastActivity().after(this.lastActivity)) ) {
                this.lastActivity = qs.getLastActivity();
            }
            if (qs.getInDay() > 0) {
                this.inDay += qs.getInDay();
            }
            if (qs.getInHour() > 0) {
                this.inHour += qs.getInHour();
            }
            if (qs.getOutDay() > 0) {
                this.outDay += qs.getOutDay();
            }
            if (qs.getOutDay() > 0) {
                this.outHour += qs.getOutDay();
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return "QueueStatVO{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", lastActivity=" + lastActivity +
                ", inHour=" + inHour +
                ", outHour=" + outHour +
                ", inDay=" + inDay +
                ", outDay=" + outDay +
                ", nodes=" + nodes +
                ", local=" + local +
                "} ";
    }
}

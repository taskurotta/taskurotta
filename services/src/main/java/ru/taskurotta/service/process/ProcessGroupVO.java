package ru.taskurotta.service.process;

import java.util.Collection;

/**
 * POJO representing group of broken processes
 * User: dimadin
 * Date: 16.10.13 11:58
 */
public class ProcessGroupVO {

    private String name;
    private Collection<String> processIds;
    private int total;
    private int exceptionsCount;
    private int actorsCount;
    private int startersCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getExceptionsCount() {
        return exceptionsCount;
    }

    public void setExceptionsCount(int exceptionsCount) {
        this.exceptionsCount = exceptionsCount;
    }

    public int getActorsCount() {
        return actorsCount;
    }

    public void setActorsCount(int actorsCount) {
        this.actorsCount = actorsCount;
    }

    public int getStartersCount() {
        return startersCount;
    }

    public void setStartersCount(int startersCount) {
        this.startersCount = startersCount;
    }

    public Collection<String> getProcessIds() {
        return processIds;
    }

    public void setProcessIds(Collection<String> processIds) {
        this.processIds = processIds;
    }

    @Override
    public String toString() {
        return "ProcessGroupVO{" +
                "name='" + name + '\'' +
                ", total=" + total +
                ", exceptionsCount=" + exceptionsCount +
                ", actorsCount=" + actorsCount +
                ", startersCount=" + startersCount +
                ", processIds=" + processIds +
                "} ";
    }

}

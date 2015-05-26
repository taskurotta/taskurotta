package ru.taskurotta.service.console.model;

/**
 * POJO representing group of interrupted tasks with common properties
 * Date: 16.10.13 11:58
 */
public class TasksGroupVO {

    private String name;
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

    @Override
    public String toString() {
        return "TasksGroupVO{" +
                "name='" + name + '\'' +
                ", total=" + total +
                ", exceptionsCount=" + exceptionsCount +
                ", actorsCount=" + actorsCount +
                ", startersCount=" + startersCount +
                '}';
    }

}

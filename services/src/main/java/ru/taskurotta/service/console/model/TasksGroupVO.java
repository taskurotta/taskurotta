package ru.taskurotta.service.console.model;

import java.util.Collection;

/**
 * POJO representing group of interrupted tasks with common properties
 * User: dimadin
 * Date: 16.10.13 11:58
 */
public class TasksGroupVO {

    private String name;
    private Collection<TaskIdentifier> tasks;
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

    public Collection<TaskIdentifier> getTasks() {
        return tasks;
    }

    public void setTasks(Collection<TaskIdentifier> tasks) {
        this.tasks = tasks;
    }

    @Override
    public String toString() {
        return "TasksGroupVO{" +
                "name='" + name + '\'' +
                ", tasks=" + tasks +
                ", total=" + total +
                ", exceptionsCount=" + exceptionsCount +
                ", actorsCount=" + actorsCount +
                ", startersCount=" + startersCount +
                '}';
    }

}

package ru.taskurotta.service.notification.model;

import ru.taskurotta.service.console.model.InterruptedTask;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created on 08.06.2015.
 */
public class StoredState implements Serializable {
    private long id;
    private List<String> queues;
    private List<InterruptedTask> tasks;
    private Date date;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<String> getQueues() {
        return queues;
    }

    public void setQueues(List<String> queues) {
        this.queues = queues;
    }

    public List<InterruptedTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<InterruptedTask> tasks) {
        this.tasks = tasks;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "StoredState{" +
                "queues=" + queues +
                ", tasks=" + tasks +
                ", date=" + date +
                '}';
    }
}

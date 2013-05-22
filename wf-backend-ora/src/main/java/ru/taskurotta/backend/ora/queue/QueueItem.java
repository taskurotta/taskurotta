package ru.taskurotta.backend.ora.queue;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Enqueued task representation
 * User: dimadin
 * Date: 22.05.13 15:36
 */
public class QueueItem {
    private UUID id;
    private String taskList;
    private int status;
    private Timestamp startDate;
    private Timestamp insertDate;

    public UUID getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(Timestamp insertDate) {
        this.insertDate = insertDate;
    }

    @Override
    public String toString() {
        return "QueueItem{" +
                "id=" + id +
                ", taskList='" + taskList + '\'' +
                ", status=" + status +
                ", startDate=" + startDate +
                ", insertDate=" + insertDate +
                '}';
    }
}

package ru.taskurotta.oracle.test.domain;

import ru.taskurotta.server.model.TaskObject;

import java.util.Date;
import java.util.UUID;

/**
 * User: greg
 */
public class SimpleTask {

    private UUID taskId;
    private int typeId;
    private Date date;
    private int statusId;
    private String actorId;


    public SimpleTask(UUID taskId, int typeId, Date date, int statusId, String actorId) {
        this.taskId = taskId;
        this.typeId = typeId;
        this.date = date;
        this.statusId = statusId;
        this.actorId = actorId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public static SimpleTask createFromTaskObject(TaskObject taskObject){
       return new SimpleTask(
               taskObject.getTaskId(),   //temporal solution
               taskObject.getTarget().getType().ordinal(),
               new Date(taskObject.getState().getTime()),
               taskObject.getState().getState().ordinal(),
               ""
       );
    }
}

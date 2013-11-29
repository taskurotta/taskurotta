package ru.taskurotta.schedule.adapter;

import java.io.Serializable;

/**
 * Action message for HZ topic
 * User: dimadin
 * Date: 24.09.13 17:52
 */
public class HzMessage implements Serializable {

    private long id;
    private String action;

    public HzMessage(){}

    public HzMessage(long id, String action) {
        this.id = id;
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "HzMessage{" +
                "id=" + id +
                ", action='" + action + '\'' +
                "} ";
    }
}

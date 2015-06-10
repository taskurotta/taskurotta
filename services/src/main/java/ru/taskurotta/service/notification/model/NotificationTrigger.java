package ru.taskurotta.service.notification.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created on 10.06.2015.
 */
public class NotificationTrigger implements Serializable {

    public static final String TYPE_VOID_QUEUES = "voidQueues";
    public static final String TYPE_FAILED_TASKS = "failedTasks";

    private long id;
    private Date changeDate;
    private String type;
    private String storedState;
    private String cfg;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(Date changeDate) {
        this.changeDate = changeDate;
    }

    public String getStoredState() {
        return storedState;
    }

    public void setStoredState(String storedState) {
        this.storedState = storedState;
    }

    public String getCfg() {
        return cfg;
    }

    public void setCfg(String cfg) {
        this.cfg = cfg;
    }

    @Override
    public String toString() {
        return "NotificationTrigger{" +
                "id=" + id +
                ", changeDate=" + changeDate +
                ", type='" + type + '\'' +
                ", storedState='" + storedState + '\'' +
                ", cfg='" + cfg + '\'' +
                '}';
    }
}

package ru.taskurotta.service.notification.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created on 08.06.2015.
 */
public class NotificationConfig implements Serializable {

    private long id;

    private List<String> actorIds;

    private List<String> emails;

    private String type;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<String> getActorIds() {
        return actorIds;
    }

    public void setActorIds(List<String> actorIds) {
        this.actorIds = actorIds;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "NotificationConfig{" +
                "id=" + id +
                ", actorIds=" + actorIds +
                ", emails=" + emails +
                ", type='" + type + '\'' +
                '}';
    }
}

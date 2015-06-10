package ru.taskurotta.service.notification.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created on 08.06.2015.
 */
public class Subscription implements Serializable {

    private long id;

    private List<String> actorIds;

    private List<String> emails;

    private List<Long> triggersKeys;

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

    public List<Long> getTriggersKeys() {
        return triggersKeys;
    }

    public void setTriggersKeys(List<Long> triggersKeys) {
        this.triggersKeys = triggersKeys;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "id=" + id +
                ", actorIds=" + actorIds +
                ", emails=" + emails +
                ", triggersKeys=" + triggersKeys +
                '}';
    }
}

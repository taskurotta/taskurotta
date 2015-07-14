package ru.taskurotta.service.notification.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Represents link of the emails to a given actor id. Meaning that this emails are interested in receiving
 * notifications about problems detected for the given actors.
 *
 * Every actorId is actualy a *starts with* prefix
 *
 * Created on 08.06.2015.
 */
public class Subscription implements Serializable {

    private long id;
    private List<String> actorIds;
    private List<String> emails;
    private List<Long> triggersKeys;
    private Date changeDate;
    private String script;

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

    public Date getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(Date changeDate) {
        this.changeDate = changeDate;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "id=" + id +
                ", actorIds=" + actorIds +
                ", emails=" + emails +
                ", triggersKeys=" + triggersKeys +
                ", changeDate=" + changeDate +
                ", script='" + script + '\'' +
                '}';
    }
}

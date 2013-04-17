package ru.taskurotta.backend.config.model;

import java.util.Properties;

public class ActorPreferences {

    private String id;
    private String type;
    private boolean blocked = false;
    private Properties timeoutPolicies;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public boolean isBlocked() {
        return blocked;
    }
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public Properties getTimeoutPolicies() {
        return timeoutPolicies;
    }
    public void setTimeoutPolicies(Properties timeoutPolicies) {
        this.timeoutPolicies = timeoutPolicies;
    }
    @Override
    public String toString() {
        return "ActorPreferences [id=" + id + ", type=" + type + ", blocked="
                + blocked + ", timeoutPolicies=" + timeoutPolicies + "]";
    }

}

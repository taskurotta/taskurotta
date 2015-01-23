package ru.taskurotta.service.config.model;

/**
 * Server-side preferences of registered actor
 */
public class ActorPreferences {

    private String id;
    private boolean blocked = false;
    private String queueName;
    private long keepTime;

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

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public long getKeepTime() {
        return keepTime;
    }

    public void setKeepTime(long keepTime) {
        this.keepTime = keepTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActorPreferences that = (ActorPreferences) o;

        if (blocked != that.blocked) return false;
        if (keepTime != that.keepTime) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (queueName != null ? !queueName.equals(that.queueName) : that.queueName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (blocked ? 1 : 0);
        result = 31 * result + (queueName != null ? queueName.hashCode() : 0);
        result = 31 * result + (int) (keepTime ^ (keepTime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ActorPreferences{" +
                "id='" + id + '\'' +
                ", blocked=" + blocked +
                ", queueName='" + queueName + '\'' +
                ", keepTime=" + keepTime +
                '}';
    }
}

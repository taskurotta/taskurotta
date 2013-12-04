package ru.taskurotta.backend.hz.queue.delay;

import java.io.Serializable;

/**
* User: stukushin
* Date: 04.12.13
* Time: 18:57
*/
class StorageItem implements Serializable {

    private Object object;
    private String queueName;
    private long keepTime;

    StorageItem(Object object, String queueName, long keepBeforeTime) {
        this.object = object;
        this.queueName = queueName;
        this.keepTime = keepBeforeTime;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
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

        StorageItem that = (StorageItem) o;

        if (keepTime != that.keepTime) return false;
        if (object != null ? !object.equals(that.object) : that.object != null) return false;
        if (queueName != null ? !queueName.equals(that.queueName) : that.queueName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = object != null ? object.hashCode() : 0;
        result = 31 * result + (queueName != null ? queueName.hashCode() : 0);
        result = 31 * result + (int) (keepTime ^ (keepTime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "StorageItem{" +
                "object=" + object +
                ", queueName='" + queueName + '\'' +
                ", keepBeforeTime=" + keepTime +
                '}';
    }
}

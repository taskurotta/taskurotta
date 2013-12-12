package ru.taskurotta.hazelcast.queue.delay;

/**
 * User: stukushin
 * Date: 05.12.13
 * Time: 11:38
 */
public class StorageItem {

    private Object object;
    private long enqueueTime;
    private String queueName;

    public StorageItem(Object object, long enqueueTime, String queueName) {
        this.object = object;
        this.enqueueTime = enqueueTime;
        this.queueName = queueName;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public long getEnqueueTime() {
        return enqueueTime;
    }

    public void setEnqueueTime(long enqueueTime) {
        this.enqueueTime = enqueueTime;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageItem that = (StorageItem) o;

        if (enqueueTime != that.enqueueTime) return false;
        if (object != null ? !object.equals(that.object) : that.object != null) return false;
        if (queueName != null ? !queueName.equals(that.queueName) : that.queueName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = object != null ? object.hashCode() : 0;
        result = 31 * result + (int) (enqueueTime ^ (enqueueTime >>> 32));
        result = 31 * result + (queueName != null ? queueName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StorageItem{" +
                "object=" + object +
                ", enqueueTime=" + enqueueTime +
                ", queueName='" + queueName + '\'' +
                '}';
    }
}

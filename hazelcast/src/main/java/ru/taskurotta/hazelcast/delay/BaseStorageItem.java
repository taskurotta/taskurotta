package ru.taskurotta.hazelcast.delay;

import java.io.Serializable;

/**
* User: stukushin
* Date: 04.12.13
* Time: 18:57
*/
public class BaseStorageItem implements Serializable {

    private Object object;
    private long enqueueTime;

    public BaseStorageItem(Object object, long enqueueTime) {
        this.object = object;
        this.enqueueTime = enqueueTime;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseStorageItem that = (BaseStorageItem) o;

        if (enqueueTime != that.enqueueTime) return false;
        if (object != null ? !object.equals(that.object) : that.object != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = object != null ? object.hashCode() : 0;
        result = 31 * result + (int) (enqueueTime ^ (enqueueTime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "BaseStorageItem{" +
                "object=" + object +
                ", enqueueTime=" + enqueueTime +
                '}';
    }
}

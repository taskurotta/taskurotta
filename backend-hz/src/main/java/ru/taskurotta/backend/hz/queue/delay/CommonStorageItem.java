package ru.taskurotta.backend.hz.queue.delay;

/**
 * User: stukushin
 * Date: 05.12.13
 * Time: 11:38
 */
public class CommonStorageItem extends BaseStorageItem {

    private String queueName;

    public CommonStorageItem(Object object, long enqueueTime, String queueName) {
        super(object, enqueueTime);
        this.queueName = queueName;
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
        if (!super.equals(o)) return false;

        CommonStorageItem that = (CommonStorageItem) o;

        if (queueName != null ? !queueName.equals(that.queueName) : that.queueName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (queueName != null ? queueName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CommonStorageItem{" +
                "queueName='" + queueName + '\'' +
                "} " + super.toString();
    }
}

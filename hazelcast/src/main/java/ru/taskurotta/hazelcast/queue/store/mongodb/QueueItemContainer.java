package ru.taskurotta.hazelcast.queue.store.mongodb;

/**
 */
public class QueueItemContainer {

    private long id;
    private Object queueItem;

    public QueueItemContainer() {
    }

    public QueueItemContainer(long id, Object queueItem) {
        this.id = id;
        this.queueItem = queueItem;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Object getQueueItem() {
        return queueItem;
    }

    public void setQueueItem(Object queueItem) {
        this.queueItem = queueItem;
    }
}

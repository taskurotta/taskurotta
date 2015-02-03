package ru.taskurotta.hazelcast.queue.store.mongodb.bson;

import ru.taskurotta.hazelcast.queue.store.mongodb.QueueItemContainer;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 */
public class QueueItemContainerStreamBSerializer implements StreamBSerializer<QueueItemContainer> {

    public static final CString OBJ = new CString("o");

    StreamBSerializer objectSerializer;

    public QueueItemContainerStreamBSerializer(StreamBSerializer objectSerializer) {
        this.objectSerializer = objectSerializer;
    }


    @Override
    public Class<QueueItemContainer> getObjectClass() {
        return QueueItemContainer.class;
    }

    @Override
    public void write(BDataOutput out, QueueItemContainer queueItemContainer) {
        out.writeLong(_ID, queueItemContainer.getId());

        int label = out.writeObject(OBJ);

        objectSerializer.write(out,  queueItemContainer.getQueueItem());

        out.writeObjectStop(label);
    }

    @Override
    public QueueItemContainer read(BDataInput in) {

        QueueItemContainer queueItemContainer = new QueueItemContainer();

        queueItemContainer.setId(in.readLong(_ID));

        int label = in.readObject(OBJ);
        if (label != -1) {

            queueItemContainer.setQueueItem(objectSerializer.read(in));
            in.readObjectStop(label);
        }

        return queueItemContainer;
    }
}

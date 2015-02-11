package ru.taskurotta.hazelcast.queue.delay.impl.mongodb;

import ru.taskurotta.hazelcast.queue.delay.impl.StorageItemContainer;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 */
public class StorageItemContainerBSerializer implements StreamBSerializer<StorageItemContainer> {



    public static final CString ENQUEUE_TIME = new CString("eTime");
    public static final CString OBJ = new CString("o");

    StreamBSerializer objectSerializer;

    public StorageItemContainerBSerializer(StreamBSerializer objectSerializer) {
        this.objectSerializer = objectSerializer;
    }


    @Override
    public Class<StorageItemContainer> getObjectClass() {
        return StorageItemContainer.class;
    }

    @Override
    public void write(BDataOutput out, StorageItemContainer storageItemContainer) {
        out.writeUUID(_ID, storageItemContainer.getId());
        out.writeLong(ENQUEUE_TIME, storageItemContainer.getEnqueueTime());

        int label = out.writeObject(OBJ);

        objectSerializer.write(out, storageItemContainer.getObject());

        out.writeObjectStop(label);
    }

    @Override
    public StorageItemContainer read(BDataInput in) {

        StorageItemContainer storageItemContainer = new StorageItemContainer();

        storageItemContainer.setId(in.readUUID(_ID));
        storageItemContainer.setEnqueueTime(in.readLong(ENQUEUE_TIME));

        int label = in.readObject(OBJ);
        if (label != -1) {

            storageItemContainer.setObject(objectSerializer.read(in));
            in.readObjectStop(label);
        }

        return storageItemContainer;
    }
}

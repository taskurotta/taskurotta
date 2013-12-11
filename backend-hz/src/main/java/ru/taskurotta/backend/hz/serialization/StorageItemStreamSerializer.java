package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.hazelcast.delay.StorageItem;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 06.12.13
 * Time: 14:58
 */
public class StorageItemStreamSerializer implements StreamSerializer<StorageItem> {

    @Override
    public void write(ObjectDataOutput out, StorageItem object) throws IOException {
        out.writeObject(object.getObject());
        out.writeLong(object.getEnqueueTime());
        out.writeUTF(object.getQueueName());
    }

    @Override
    public StorageItem read(ObjectDataInput in) throws IOException {

        Object object = in.readObject();
        long enqueueTime = in.readLong();
        String queueName = in.readUTF();

        return new StorageItem(object, enqueueTime, queueName);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.COMMON_STORAGE_ITEM;
    }

    @Override
    public void destroy() {

    }
}

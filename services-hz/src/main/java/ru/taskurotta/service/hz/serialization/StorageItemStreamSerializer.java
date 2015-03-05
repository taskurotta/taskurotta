package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.hazelcast.queue.delay.impl.StorageItemContainer;

import java.io.IOException;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 06.12.13
 * Time: 14:58
 */
public class StorageItemStreamSerializer implements StreamSerializer<StorageItemContainer> {

    @Override
    public void write(ObjectDataOutput out, StorageItemContainer object) throws IOException {
        UUIDSerializer.write(out, object.getId());
        out.writeObject(object.getObject());
        out.writeLong(object.getEnqueueTime());
        out.writeUTF(object.getQueueName());
    }

    @Override
    public StorageItemContainer read(ObjectDataInput in) throws IOException {
        UUID id = UUIDSerializer.read(in);
        Object object = in.readObject();
        long enqueueTime = in.readLong();
        String queueName = in.readUTF();

        return new StorageItemContainer(id, object, enqueueTime, queueName);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.STORAGE_ITEM;
    }

    @Override
    public void destroy() {

    }
}

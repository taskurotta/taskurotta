package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.backend.hz.queue.delay.CommonStorageItem;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 06.12.13
 * Time: 14:58
 */
public class CommonStorageItemStreamSerializer implements StreamSerializer<CommonStorageItem> {

    @Override
    public void write(ObjectDataOutput out, CommonStorageItem object) throws IOException {
        out.writeObject(object.getObject());
        out.writeLong(object.getEnqueueTime());
        out.writeUTF(object.getQueueName());
    }

    @Override
    public CommonStorageItem read(ObjectDataInput in) throws IOException {

        Object object = in.readObject();
        long enqueueTime = in.readLong();
        String queueName = in.readUTF();

        return new CommonStorageItem(object, enqueueTime, queueName);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.COMMON_STORAGE_ITEM;
    }

    @Override
    public void destroy() {

    }
}

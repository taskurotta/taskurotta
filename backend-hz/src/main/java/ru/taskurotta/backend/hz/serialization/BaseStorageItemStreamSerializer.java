package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.backend.hz.queue.delay.BaseStorageItem;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 06.12.13
 * Time: 14:53
 */
public class BaseStorageItemStreamSerializer implements StreamSerializer<BaseStorageItem> {

    @Override
    public void write(ObjectDataOutput out, BaseStorageItem object) throws IOException {
        out.writeObject(object.getObject());
        out.writeLong(object.getEnqueueTime());
    }

    @Override
    public BaseStorageItem read(ObjectDataInput in) throws IOException {
        Object object = in.readObject();
        long enqueueTime = in.readLong();

        return new BaseStorageItem(object, enqueueTime);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.BASE_STORAGE_ITEM;
    }

    @Override
    public void destroy() {

    }
}

package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.hz.config.ComputeQueueBalanceTask;

import java.io.IOException;

/**
 * Created on 15.05.2015.
 */
public class ComputeQueueBalanceTaskStreamSerializer implements StreamSerializer<ComputeQueueBalanceTask> {

    @Override
    public void write(ObjectDataOutput out, ComputeQueueBalanceTask object) throws IOException {
        SerializationTools.writeString(out, object.getQueueName());
    }

    @Override
    public ComputeQueueBalanceTask read(ObjectDataInput in) throws IOException {
        return new ComputeQueueBalanceTask(SerializationTools.readString(in));
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.HZ_QUEUE_BALANCE_TASK;
    }

    @Override
    public void destroy() {

    }
}

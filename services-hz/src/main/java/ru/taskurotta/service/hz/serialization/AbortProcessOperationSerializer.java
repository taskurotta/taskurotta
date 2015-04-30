package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.recovery.AbortProcessOperation;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 20.04.2015
 * Time: 19:01
 */
public class AbortProcessOperationSerializer implements StreamSerializer<AbortProcessOperation> {

    @Override
    public void write(ObjectDataOutput out, AbortProcessOperation object) throws IOException {
        UUIDSerializer.write(out, object.getProcessId());
    }

    @Override
    public AbortProcessOperation read(ObjectDataInput in) throws IOException {
        return new AbortProcessOperation(UUIDSerializer.read(in));
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.ABORT_OPERATION;
    }

    @Override
    public void destroy() {

    }
}

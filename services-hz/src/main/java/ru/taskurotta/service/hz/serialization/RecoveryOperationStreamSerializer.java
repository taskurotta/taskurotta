package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.recovery.RecoveryOperation;

import java.io.IOException;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 22.01.2015
 * Time: 19:53
 */

public class RecoveryOperationStreamSerializer implements StreamSerializer<RecoveryOperation> {

    @Override
    public void write(ObjectDataOutput out, RecoveryOperation object) throws IOException {
        UUID uuid = object.getProcessId();
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    @Override
    public RecoveryOperation read(ObjectDataInput in) throws IOException {
        long msb = in.readLong();
        long lsb = in.readLong();
        return new RecoveryOperation(new UUID(msb, lsb));
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.RECOVERY_OPERATION;
    }

    @Override
    public void destroy() {}
}

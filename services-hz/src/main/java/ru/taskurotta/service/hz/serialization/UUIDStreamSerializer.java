package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;
import java.util.UUID;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 12:57 AM
 */
public class UUIDStreamSerializer implements StreamSerializer<UUID> {

    @Override
    public void write(ObjectDataOutput out, UUID uuid) throws IOException {
        out.writeLong(uuid.getLeastSignificantBits());
        out.writeLong(uuid.getMostSignificantBits());
    }

    @Override
    public UUID read(ObjectDataInput in) throws IOException {
        long lsb = in.readLong();
        long msb = in.readLong();
        return new UUID(msb, lsb);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.UUID;
    }

    @Override
    public void destroy() {

    }
}

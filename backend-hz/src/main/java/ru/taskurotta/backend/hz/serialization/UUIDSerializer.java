package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import java.io.IOException;
import java.util.UUID;

/**
 * User: romario
 * Date: 9/12/13
 * Time: 2:58 PM
 */
public class UUIDSerializer {


    public static void write(ObjectDataOutput out, UUID uuid) throws IOException {
        out.writeLong(uuid.getLeastSignificantBits());
        out.writeLong(uuid.getMostSignificantBits());
    }

    public static UUID read(ObjectDataInput in) throws IOException {
        long lsb = in.readLong();
        long msb = in.readLong();
        return new UUID(msb, lsb);
    }
}

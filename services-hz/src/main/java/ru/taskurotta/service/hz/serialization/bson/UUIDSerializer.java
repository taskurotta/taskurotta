package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.util.UUID;

/**
 * Created by greg on 05/02/15.
 */
public class UUIDSerializer implements StreamBSerializer<UUID> {

    public static final CString VALUE = new CString("v");

    @Override
    public Class<UUID> getObjectClass() {
        return UUID.class;
    }

    @Override
    public void write(BDataOutput out, UUID object) {
        out.writeUUID(VALUE, object);
    }

    @Override
    public UUID read(BDataInput in) {
        return in.readUUID(VALUE);
    }
}

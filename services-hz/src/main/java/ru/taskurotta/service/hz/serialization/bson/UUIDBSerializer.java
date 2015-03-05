package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.util.UUID;

/**
 */
public class UUIDBSerializer implements StreamBSerializer<UUID> {
    @Override
    public Class<UUID> getObjectClass() {
        return UUID.class;
    }

    @Override
    public void write(BDataOutput out, UUID object) {
        out.writeUUID(_ID, object);
    }

    @Override
    public UUID read(BDataInput in) {
        return in.readUUID(_ID);
    }
}

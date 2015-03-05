package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.config.model.ActorPreferences;

public class ActorPreferencesBSerializer implements StreamBSerializer<ActorPreferences> {

    public static final CString BLOCKED = new CString("blocked");
    public static final CString QUEUE_NAME = new CString("queueName");
    public static final CString KEEP_TIME = new CString("keepTime");

    @Override
    public Class<ActorPreferences> getObjectClass() {
        return ActorPreferences.class;
    }

    @Override
    public void write(BDataOutput out, ActorPreferences object) {
        out.writeString(_ID, object.getId());
        out.writeBoolean(BLOCKED, object.isBlocked());
        out.writeString(QUEUE_NAME, object.getQueueName());
        out.writeLong(KEEP_TIME, object.getKeepTime());
    }

    @Override
    public ActorPreferences read(BDataInput in) {
        ActorPreferences actorPreferences = new ActorPreferences();
        actorPreferences.setId(in.readString(_ID));
        actorPreferences.setBlocked(in.readBoolean(BLOCKED));
        actorPreferences.setQueueName(in.readString(QUEUE_NAME));
        actorPreferences.setKeepTime(in.readLong(KEEP_TIME));
        return actorPreferences;
    }
}

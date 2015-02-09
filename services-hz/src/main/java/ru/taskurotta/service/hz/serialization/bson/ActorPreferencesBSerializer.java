package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.config.model.ActorPreferences;

/**
 * Created by greg on 09/02/15.
 */
public class ActorPreferencesBSerializer implements StreamBSerializer<ActorPreferences> {

    public static final CString BLOCKED = new CString("b");
    public static final CString QUEUE_NAME = new CString("q");
    public static final CString KEEP_TIME = new CString("t");

    @Override
    public Class<ActorPreferences> getObjectClass() {
        return ActorPreferences.class;
    }

    @Override
    public void write(BDataOutput out, ActorPreferences object) {
        out.writeString(_ID, object.getId());
        out.writeInt(BLOCKED, object.isBlocked() ? 1 : 0);
        out.writeString(QUEUE_NAME, object.getQueueName());
        out.writeLong(KEEP_TIME, object.getKeepTime());
    }

    @Override
    public ActorPreferences read(BDataInput in) {
        ActorPreferences actorPreferences = new ActorPreferences();
        actorPreferences.setId(in.readString(_ID));
        actorPreferences.setBlocked(in.readInt(BLOCKED) == 1);
        actorPreferences.setQueueName(in.readString(QUEUE_NAME));
        actorPreferences.setKeepTime(in.readLong(KEEP_TIME));
        return actorPreferences;
    }
}

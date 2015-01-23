package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.config.model.ActorPreferences;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 23.01.2015
 * Time: 12:41
 */

public class ActorPreferencesStreamSerializer implements StreamSerializer<ActorPreferences> {
    @Override
    public void write(ObjectDataOutput out, ActorPreferences object) throws IOException {
        out.writeUTF(object.getId());
        out.writeBoolean(object.isBlocked());
        out.writeUTF(object.getQueueName());
        out.writeLong(object.getKeepTime());
    }

    @Override
    public ActorPreferences read(ObjectDataInput in) throws IOException {
        ActorPreferences actorPreferences = new ActorPreferences();
        actorPreferences.setId(in.readUTF());
        actorPreferences.setBlocked(in.readBoolean());
        actorPreferences.setQueueName(in.readUTF());
        actorPreferences.setKeepTime(in.readLong());
        return actorPreferences;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.ACTOR_PREFERENCES;
    }

    @Override
    public void destroy() {

    }
}

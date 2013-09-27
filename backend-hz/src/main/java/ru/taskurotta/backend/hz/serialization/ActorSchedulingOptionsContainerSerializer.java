package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;

import java.io.IOException;

import static ru.taskurotta.backend.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.backend.hz.serialization.SerializationTools.writeString;

/**
 * User: greg
 */
public class ActorSchedulingOptionsContainerSerializer implements StreamSerializer<ActorSchedulingOptionsContainer> {

    @Override
    public void write(ObjectDataOutput out, ActorSchedulingOptionsContainer object) throws IOException {
        writeString(out, object.getCustomId());
        out.writeLong(object.getStartTime());
        writeString(out, object.getTaskList());
    }

    @Override
    public ActorSchedulingOptionsContainer read(ObjectDataInput in) throws IOException {
        ActorSchedulingOptionsContainer container = new ActorSchedulingOptionsContainer();
        container.setCustomId(readString(in));
        container.setStartTime(in.readLong());
        container.setTaskList(readString(in));
        return container;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.ACTOR_SCHEDULING_OPTIONS_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ArgType;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.taskurotta.backend.hz.serialization.SerializationTools.readArgsContainerArray;
import static ru.taskurotta.backend.hz.serialization.SerializationTools.writeArgsContainerArray;

/**
 * User: greg
 */
public class TaskOptionsContainerSerializer implements StreamSerializer<TaskOptionsContainer> {

    private ActorSchedulingOptionsContainerStreamSerializer actorSchedulingOptionsContainerStreamSerializer = new ActorSchedulingOptionsContainerStreamSerializer();


    @Override
    public void write(ObjectDataOutput out, TaskOptionsContainer object) throws IOException {
        if (object.getActorSchedulingOptions() != null) {
            out.writeBoolean(true);
            actorSchedulingOptionsContainerStreamSerializer.write(out, object.getActorSchedulingOptions());
        } else {
            out.writeBoolean(false);
        }
        int argTypesCount = (object.getArgTypes() != null) ? object.getArgTypes().length : -1;
        if (argTypesCount > 0) {
            out.writeInt(argTypesCount);
            for (ArgType i : object.getArgTypes()) {
                out.writeInt(i.getValue());
            }
        } else {
            out.writeInt(-1);
        }

        writeArgsContainerArray(out, object.getPromisesWaitFor());
    }

    @Override
    public TaskOptionsContainer read(ObjectDataInput in) throws IOException {
        ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = null;
        if (in.readBoolean()) {
            actorSchedulingOptionsContainer = actorSchedulingOptionsContainerStreamSerializer.read(in);
        }
        int argTypesCount = in.readInt();
        List<ArgType> argTypeList = new ArrayList<>();
        ArgType[] argTypeArray = null;
        if (argTypesCount != -1) {
            for (int i = 0; i < argTypesCount; i++) {
                argTypeList.add(ArgType.fromInt(in.readInt()));
            }
            argTypeArray = new ArgType[argTypeList.size()];
            argTypeList.toArray(argTypeArray);
        }

        ArgContainer[] argContainersArray = readArgsContainerArray(in);

        return new TaskOptionsContainer(argTypeArray, actorSchedulingOptionsContainer, argContainersArray);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.TASK_OPTIONS_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

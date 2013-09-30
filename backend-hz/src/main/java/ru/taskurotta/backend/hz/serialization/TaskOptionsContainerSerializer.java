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

    private ActorSchedulingOptionsContainerSerializer actorSchedulingOptionsContainerSerializer = new ActorSchedulingOptionsContainerSerializer();
    private ArgContainerSerializer argContainerSerializer = new ArgContainerSerializer();


    @Override
    public void write(ObjectDataOutput out, TaskOptionsContainer object) throws IOException {
        actorSchedulingOptionsContainerSerializer.write(out, object.getActorSchedulingOptions());
        int argTypesCount = object.getArgTypes().length;
        if (object.getArgTypes() != null && argTypesCount > 0) {
            out.writeInt(argTypesCount);
            for (int i = 0; i < argTypesCount; i++) {
                out.writeInt(object.getArgTypes()[i].getValue());
            }
        } else {
            out.writeInt(-1);
        }

        writeArgsContainerArray(out, object.getPromisesWaitFor());
    }

    @Override
    public TaskOptionsContainer read(ObjectDataInput in) throws IOException {
        ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = actorSchedulingOptionsContainerSerializer.read(in);

        int argTypesCount = in.readInt();
        List<ArgType> argTypeList = new ArrayList<>();
        if (argTypesCount != -1) {
            for (int i = 0; i < argTypesCount; i++) {
                argTypeList.add(ArgType.fromInt(in.readInt()));
            }
        }
        ArgType[] argTypeArray = new ArgType[argTypeList.size()];
        argTypeList.toArray(argTypeArray);


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

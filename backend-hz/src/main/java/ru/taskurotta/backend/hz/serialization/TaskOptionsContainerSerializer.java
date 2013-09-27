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

        int argContainersCount = object.getPromisesWaitFor().length;
        if (object.getPromisesWaitFor() != null && argContainersCount > 0) {
            out.writeInt(argContainersCount);
            for (int i = 0; i < argContainersCount; i++) {
                argContainerSerializer.write(out, object.getPromisesWaitFor()[i]);
            }
        } else {
            out.writeInt(-1);
        }
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

        int argContainersCount = in.readInt();
        List<ArgContainer> argContainerList = new ArrayList<>();
        if (argContainersCount != -1) {
            for (int i = 0; i < argContainersCount; i++) {
                argContainerList.add(argContainerSerializer.read(in));
            }
        }
        ArgContainer[] argContainersArray = new ArgContainer[argContainerList.size()];
        argContainerList.toArray(argContainersArray);

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

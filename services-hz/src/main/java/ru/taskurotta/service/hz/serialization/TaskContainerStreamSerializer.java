package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.io.IOException;
import java.util.UUID;

import static ru.taskurotta.service.hz.serialization.SerializationTools.readArgsContainerArray;
import static ru.taskurotta.service.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.service.hz.serialization.SerializationTools.readStringArray;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeArgsContainerArray;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeString;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeStringArray;

/**
 * User: greg
 */
public class TaskContainerStreamSerializer implements StreamSerializer<TaskContainer> {


    private TaskOptionsContainerSerializer taskOptionsContainerSerializer = new TaskOptionsContainerSerializer();

    @Override
    public void write(ObjectDataOutput out, TaskContainer object) throws IOException {
        UUIDSerializer.write(out, object.getTaskId());
        writeString(out, object.getMethod());
        writeString(out, object.getActorId());
        out.writeInt(object.getType().getValue());
        out.writeLong(object.getStartTime());
        out.writeInt(object.getErrorAttempts());
        writeArgsContainerArray(out, object.getArgs());
        if (object.getOptions() == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            taskOptionsContainerSerializer.write(out, object.getOptions());
        }
        out.writeBoolean(object.isUnsafe());
        writeStringArray(out, object.getFailTypes());
        UUIDSerializer.write(out, object.getProcessId());
    }

    @Override
    public TaskContainer read(ObjectDataInput in) throws IOException {
        UUID taskId = UUIDSerializer.read(in);
        String method = readString(in);
        String actorId = readString(in);
        TaskType taskType = TaskType.fromInt(in.readInt());
        long startTime = in.readLong();
        int attempts = in.readInt();
        ArgContainer[] containers = readArgsContainerArray(in);
        TaskOptionsContainer taskOptionsContainer = null;
        if (in.readBoolean()) {
            taskOptionsContainer = taskOptionsContainerSerializer.read(in);
        }
        boolean unsafe = in.readBoolean();
        String[] failTypes = readStringArray(in);
        UUID processId = UUIDSerializer.read(in);
        return new TaskContainer(taskId, processId, method, actorId, taskType, startTime, attempts, containers,
                taskOptionsContainer, unsafe, failTypes);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.TASK_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

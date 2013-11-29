package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.IOException;
import java.util.UUID;

import static ru.taskurotta.backend.hz.serialization.SerializationTools.*;

/**
 * User: greg
 */
public class DecisionContainerSerializer implements StreamSerializer<DecisionContainer> {

    private ArgContainerSerializer argContainerSerializer = new ArgContainerSerializer();
    private ErrorContainerSerializer errorContainerSerializer = new ErrorContainerSerializer();

    @Override
    public void write(ObjectDataOutput out, DecisionContainer object) throws IOException {
        UUIDSerializer.write(out, object.getTaskId());
        UUIDSerializer.write(out, object.getProcessId());
        argContainerSerializer.write(out, object.getValue());
        errorContainerSerializer.write(out, object.getErrorContainer());
        out.writeLong(object.getExecutionTime());
        out.writeLong(object.getRestartTime());
        writeString(out, object.getActorId());
        writeTaskContainerArray(out, object.getTasks());
    }

    @Override
    public DecisionContainer read(ObjectDataInput in) throws IOException {
        UUID taskId = UUIDSerializer.read(in);
        UUID processId = UUIDSerializer.read(in);
        ArgContainer value = argContainerSerializer.read(in);
        ErrorContainer errorContainer = errorContainerSerializer.read(in);
        long exTime = in.readLong();
        long reTime = in.readLong();
        String actorId = readString(in);
        TaskContainer[] taskContainers = readTaskContainerArray(in);
        return new DecisionContainer(taskId, processId, value, errorContainer, reTime, taskContainers, actorId, exTime);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.DECISION_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

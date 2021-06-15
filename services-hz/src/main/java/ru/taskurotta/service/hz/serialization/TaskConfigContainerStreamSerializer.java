package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.TaskConfigContainer;

import java.io.IOException;

import static ru.taskurotta.service.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeString;

/**
 * User: greg
 */
public class TaskConfigContainerStreamSerializer implements StreamSerializer<TaskConfigContainer> {

    private RetryPolicyConfigContainerSerializer retryPolicyConfigContainerSerializer = new RetryPolicyConfigContainerSerializer();

    @Override
    public void write(ObjectDataOutput out, TaskConfigContainer object) throws IOException {
        writeString(out, object.getCustomId());
        out.writeLong(object.getStartTime());
        writeString(out, object.getTaskList());
        writeString(out, object.getIdempotencyKey());
        if (object.getRetryPolicyConfigContainer() != null) {
            out.writeBoolean(true);
            retryPolicyConfigContainerSerializer.write(out, object.getRetryPolicyConfigContainer());
        } else {
            out.writeBoolean(false);
        }
        out.writeLong(object.getTimeout());
    }

    @Override
    public TaskConfigContainer read(ObjectDataInput in) throws IOException {
        TaskConfigContainer container = new TaskConfigContainer();
        container.setCustomId(readString(in));
        container.setStartTime(in.readLong());
        container.setTaskList(readString(in));
        container.setIdempotencyKey(readString(in));
        boolean retryPolicyExist = in.readBoolean();
        if (retryPolicyExist) {
            container.setRetryPolicyConfigContainer(retryPolicyConfigContainerSerializer.read(in));
        }
        container.setTimeout(in.readLong());

        return container;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.TASK_CONFIG_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

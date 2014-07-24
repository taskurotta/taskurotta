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

    private RetryPolicySettingsSerializer retryPolicySettingsSerializer = new RetryPolicySettingsSerializer();

    @Override
    public void write(ObjectDataOutput out, TaskConfigContainer object) throws IOException {
        writeString(out, object.getCustomId());
        out.writeLong(object.getStartTime());
        writeString(out, object.getTaskList());
        if (object.getRetryPolicySettings() != null) {
            out.writeBoolean(true);
            retryPolicySettingsSerializer.write(out, object.getRetryPolicySettings());
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public TaskConfigContainer read(ObjectDataInput in) throws IOException {
        TaskConfigContainer container = new TaskConfigContainer();
        container.setCustomId(readString(in));
        container.setStartTime(in.readLong());
        container.setTaskList(readString(in));
        boolean retryPolicyExist = in.readBoolean();
        if (retryPolicyExist) {
            container.setRetryPolicySettings(retryPolicySettingsSerializer.read(in));
        }
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

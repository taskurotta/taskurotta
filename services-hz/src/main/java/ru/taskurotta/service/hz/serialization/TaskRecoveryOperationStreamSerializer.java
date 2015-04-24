package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.recovery.TaskRecoveryOperation;

import java.io.IOException;
import java.util.UUID;

/**
 * Created on 24.04.2015.
 */
public class TaskRecoveryOperationStreamSerializer implements StreamSerializer<TaskRecoveryOperation> {

    @Override
    public void write(ObjectDataOutput out, TaskRecoveryOperation object) throws IOException {
        UUIDSerializer.write(out, object.getProcessId());
        UUIDSerializer.write(out, object.getTaskId());
    }

    @Override
    public TaskRecoveryOperation read(ObjectDataInput in) throws IOException {
        UUID processId = UUIDSerializer.read(in);
        UUID taskId = UUIDSerializer.read(in);
        return new TaskRecoveryOperation(processId, taskId);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.TASK_RECOVERY_OPERATION;
    }

    @Override
    public void destroy() {}

}

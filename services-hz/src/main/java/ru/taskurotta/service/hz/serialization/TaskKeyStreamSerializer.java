package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.hz.TaskKey;

import java.io.IOException;

/**
 */
public class TaskKeyStreamSerializer implements StreamSerializer<TaskKey> {

    @Override
    public void write(ObjectDataOutput out, TaskKey taskKey) throws IOException {
        UUIDSerializer.write(out, taskKey.getTaskId());
        UUIDSerializer.write(out, taskKey.getProcessId());
    }

    @Override
    public TaskKey read(ObjectDataInput in) throws IOException {
        return new TaskKey(UUIDSerializer.read(in), UUIDSerializer.read(in));
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.TASK_KEY;
    }

    @Override
    public void destroy() {

    }
}

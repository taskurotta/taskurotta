package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.hz.TaskKey;

import java.io.IOException;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 10:37 PM
 */
public class TaskKeyStreamSerializer implements StreamSerializer<TaskKey> {

    @Override
    public void write(ObjectDataOutput out, TaskKey taskKey) throws IOException {
        UUIDSerializer.write(out, taskKey.getProcessId());
        UUIDSerializer.write(out, taskKey.getTaskId());
    }

    @Override
    public TaskKey read(ObjectDataInput in) throws IOException {

        TaskKey taskKey = new TaskKey();
        taskKey.setProcessId(UUIDSerializer.read(in));
        taskKey.setTaskId(UUIDSerializer.read(in));

        return taskKey;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.TASK_KEY;
    }

    @Override
    public void destroy() {

    }
}

package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.hz.TaskFatKey;

import java.io.IOException;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 10:37 PM
 */
public class TaskFatKeyStreamSerializer implements StreamSerializer<TaskFatKey> {

    @Override
    public void write(ObjectDataOutput out, TaskFatKey taskFatKey) throws IOException {
        UUIDSerializer.write(out, taskFatKey.getProcessId());
        UUIDSerializer.write(out, taskFatKey.getTaskId());
    }

    @Override
    public TaskFatKey read(ObjectDataInput in) throws IOException {

        TaskFatKey taskFatKey = new TaskFatKey();
        taskFatKey.setProcessId(UUIDSerializer.read(in));
        taskFatKey.setTaskId(UUIDSerializer.read(in));

        return taskFatKey;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.FAT_TASK_KEY;
    }

    @Override
    public void destroy() {

    }
}

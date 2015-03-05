package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.hz.TaskKey;

import java.util.UUID;

/**
 */
public class TaskKeyBSerializer implements StreamBSerializer<TaskKey> {

    private static final CString TASK_ID = new CString("t");
    private static final CString PROCESS_ID = new CString("p");

    @Override
    public Class<TaskKey> getObjectClass() {
        return TaskKey.class;
    }

    @Override
    public void write(BDataOutput out, TaskKey object) {
        int label = out.writeObject(_ID);
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeObjectStop(label);

    }

    @Override
    public TaskKey read(BDataInput in) {
        int label = in.readObject(_ID);
        final UUID taskId = in.readUUID(TASK_ID);
        final UUID processId = in.readUUID(PROCESS_ID);
        in.readObjectStop(label);

        return new TaskKey(taskId, processId);
    }
}

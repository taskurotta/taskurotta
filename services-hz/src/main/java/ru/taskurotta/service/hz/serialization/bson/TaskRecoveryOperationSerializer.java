package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.recovery.TaskRecoveryOperation;

/**
 * Created on 24.04.2015.
 */
public class TaskRecoveryOperationSerializer implements StreamBSerializer<TaskRecoveryOperation> {

    public static final CString PROCESS_ID = new CString("pId");
    public static final CString TASK_ID = new CString("tId");

    @Override
    public Class<TaskRecoveryOperation> getObjectClass() {
        return TaskRecoveryOperation.class;
    }

    @Override
    public void write(BDataOutput out, TaskRecoveryOperation object) {
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeUUID(TASK_ID, object.getTaskId());
    }

    @Override
    public TaskRecoveryOperation read(BDataInput in) {
        return new TaskRecoveryOperation(in.readUUID(PROCESS_ID), in.readUUID(TASK_ID));
    }
}

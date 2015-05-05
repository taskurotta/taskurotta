package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.recovery.RestartTaskOperation;

/**
 * Created on 24.04.2015.
 */
public class RestartTaskOperationSerializer implements StreamBSerializer<RestartTaskOperation> {

    public static final CString PROCESS_ID = new CString("pId");
    public static final CString TASK_ID = new CString("tId");

    @Override
    public Class<RestartTaskOperation> getObjectClass() {
        return RestartTaskOperation.class;
    }

    @Override
    public void write(BDataOutput out, RestartTaskOperation object) {
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeUUID(TASK_ID, object.getTaskId());
    }

    @Override
    public RestartTaskOperation read(BDataInput in) {
        return new RestartTaskOperation(in.readUUID(PROCESS_ID), in.readUUID(TASK_ID));
    }
}

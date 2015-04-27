package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.recovery.AbortOperation;

/**
 * User: stukushin
 * Date: 20.04.2015
 * Time: 19:01
 */
public class AbortOperationSerializer implements StreamBSerializer<AbortOperation> {

    public static final CString PROCESS_ID = new CString("pId");

    @Override
    public Class<AbortOperation> getObjectClass() {
        return AbortOperation.class;
    }

    @Override
    public void write(BDataOutput out, AbortOperation object) {
        out.writeUUID(PROCESS_ID, object.getProcessId());
    }

    @Override
    public AbortOperation read(BDataInput in) {
        return new AbortOperation(in.readUUID(PROCESS_ID));
    }
}

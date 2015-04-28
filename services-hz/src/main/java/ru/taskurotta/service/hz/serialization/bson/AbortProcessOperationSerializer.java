package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.recovery.AbortProcessOperation;

/**
 * User: stukushin
 * Date: 20.04.2015
 * Time: 19:01
 */
public class AbortProcessOperationSerializer implements StreamBSerializer<AbortProcessOperation> {

    public static final CString PROCESS_ID = new CString("pId");

    @Override
    public Class<AbortProcessOperation> getObjectClass() {
        return AbortProcessOperation.class;
    }

    @Override
    public void write(BDataOutput out, AbortProcessOperation object) {
        out.writeUUID(PROCESS_ID, object.getProcessId());
    }

    @Override
    public AbortProcessOperation read(BDataInput in) {
        return new AbortProcessOperation(in.readUUID(PROCESS_ID));
    }
}

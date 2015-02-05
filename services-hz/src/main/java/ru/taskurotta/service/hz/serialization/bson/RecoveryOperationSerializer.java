package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.recovery.RecoveryOperation;

/**
 * Created by greg on 05/02/15.
 */
public class RecoveryOperationSerializer implements StreamBSerializer<RecoveryOperation> {

    public static final CString PROCESS_ID = new CString("pId");

    @Override
    public Class<RecoveryOperation> getObjectClass() {
        return RecoveryOperation.class;
    }

    @Override
    public void write(BDataOutput out, RecoveryOperation object) {
           out.writeUUID(PROCESS_ID, object.getProcessId());
    }

    @Override
    public RecoveryOperation read(BDataInput in) {
        return new RecoveryOperation(in.readUUID(PROCESS_ID));
    }
}

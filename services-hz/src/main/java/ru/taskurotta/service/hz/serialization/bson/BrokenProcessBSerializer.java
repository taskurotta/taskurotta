package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.console.model.BrokenProcess;

/**
 * Created by greg on 09/02/15.
 */
public class BrokenProcessBSerializer implements StreamBSerializer<BrokenProcess> {

    public static final CString START_ACTOR_ID = new CString("sai");
    public static final CString BROKEN_STORE_ID = new CString("bai");
    public static final CString TIME = new CString("t");
    public static final CString ERROR_MESSAGE = new CString("em");
    public static final CString ERROR_CLASS_NAME = new CString("ec");
    public static final CString STACK_TRACE = new CString("st");

    @Override
    public Class<BrokenProcess> getObjectClass() {
        return BrokenProcess.class;
    }

    @Override
    public void write(BDataOutput out, BrokenProcess object) {
        out.writeUUID(_ID, object.getProcessId());
        out.writeString(START_ACTOR_ID, object.getStartActorId());
        out.writeString(BROKEN_STORE_ID, object.getBrokenActorId());
        out.writeLong(TIME, object.getTime());
        out.writeString(ERROR_MESSAGE, object.getErrorMessage());
        out.writeString(ERROR_CLASS_NAME, object.getErrorClassName());
        out.writeString(STACK_TRACE, object.getStackTrace());
    }

    @Override
    public BrokenProcess read(BDataInput in) {
        BrokenProcess brokenProcess = new BrokenProcess();
        brokenProcess.setProcessId(in.readUUID(_ID));
        brokenProcess.setStartActorId(in.readString(START_ACTOR_ID));
        brokenProcess.setBrokenActorId(in.readString(BROKEN_STORE_ID));
        brokenProcess.setTime(in.readLong(TIME));
        brokenProcess.setErrorMessage(in.readString(ERROR_MESSAGE));
        brokenProcess.setErrorClassName(in.readString(ERROR_CLASS_NAME));
        brokenProcess.setStackTrace(in.readString(STACK_TRACE));
        return brokenProcess;
    }
}

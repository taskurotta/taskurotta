package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.console.model.InterruptedTask;

public class InterruptedTaskBSerializer implements StreamBSerializer<InterruptedTask> {

    public static final CString PROCESS_ID = new CString("processId");
    public static final CString STARTER_ID = new CString("starterId");
    public static final CString ACTOR_ID = new CString("actorId");
    public static final CString TIME = new CString("time");
    public static final CString ERROR_MESSAGE = new CString("errorMessage");
    public static final CString ERROR_CLASS_NAME = new CString("errorClassName");

    @Override
    public Class<InterruptedTask> getObjectClass() {
        return InterruptedTask.class;
    }

    @Override
    public void write(BDataOutput out, InterruptedTask object) {
        out.writeUUID(_ID, object.getTaskId());
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeString(STARTER_ID, object.getStarterId());
        out.writeString(ACTOR_ID, object.getActorId());
        out.writeLong(TIME, object.getTime());
        out.writeString(ERROR_MESSAGE, object.getErrorMessage());
        out.writeString(ERROR_CLASS_NAME, object.getErrorClassName());
    }

    @Override
    public InterruptedTask read(BDataInput in) {
        InterruptedTask result = new InterruptedTask();
        result.setTaskId(in.readUUID(_ID));
        result.setProcessId(in.readUUID(PROCESS_ID));
        result.setStarterId(in.readString(STARTER_ID));
        result.setActorId(in.readString(ACTOR_ID));
        result.setTime(in.readLong(TIME));
        result.setErrorMessage(in.readString(ERROR_MESSAGE));
        result.setErrorClassName(in.readString(ERROR_CLASS_NAME));
        return result;
    }
}

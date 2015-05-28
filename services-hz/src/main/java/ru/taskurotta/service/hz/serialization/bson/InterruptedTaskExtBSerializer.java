package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.console.model.InterruptedTaskExt;

/**
 * Created on 25.05.2015.
 */
public class InterruptedTaskExtBSerializer implements StreamBSerializer<InterruptedTaskExt> {

    public static final CString TASK_ID = new CString("taskId");
    public static final CString PROCESS_ID = new CString("processId");
    public static final CString STARTER_ID = new CString("starterId");
    public static final CString ACTOR_ID = new CString("actorId");
    public static final CString TIME = new CString("time");
    public static final CString ERROR_MESSAGE = new CString("errorMessage");
    public static final CString ERROR_CLASS_NAME = new CString("errorClassName");

    public static final CString FULL_MESSAGE = new CString("fullMessage");
    public static final CString STACK_TRACE = new CString("stackTrace");

    @Override
    public Class<InterruptedTaskExt> getObjectClass() {
        return InterruptedTaskExt.class;
    }

    @Override
    public void write(BDataOutput out, InterruptedTaskExt object) {
        out.writeUUID(_ID, object.getTaskId());
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeString(STARTER_ID, object.getStarterId());
        out.writeString(ACTOR_ID, object.getActorId());
        out.writeLong(TIME, object.getTime());
        out.writeString(ERROR_MESSAGE, object.getErrorMessage());
        out.writeString(ERROR_CLASS_NAME, object.getErrorClassName());

        out.writeString(FULL_MESSAGE, object.getFullMessage());
        out.writeString(STACK_TRACE, object.getStackTrace());
    }

    @Override
    public InterruptedTaskExt read(BDataInput in) {
        InterruptedTaskExt result = new InterruptedTaskExt();

        result.setTaskId(in.readUUID(_ID));
        result.setProcessId(in.readUUID(PROCESS_ID));
        result.setStarterId(in.readString(STARTER_ID));
        result.setActorId(in.readString(ACTOR_ID));
        result.setTime(in.readLong(TIME));
        result.setErrorMessage(in.readString(ERROR_MESSAGE));
        result.setErrorClassName(in.readString(ERROR_CLASS_NAME));

        result.setFullMessage(in.readString(FULL_MESSAGE));
        result.setStackTrace(in.readString(STACK_TRACE));
        return result;
    }
}

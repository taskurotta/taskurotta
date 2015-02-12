package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.transport.model.TaskContainer;

public class ProcessBSerializer implements StreamBSerializer<Process> {

    public static final CString CUSTOM_ID = new CString("customId");
    public static final CString START_TIME = new CString("startTime");
    public static final CString END_TIME = new CString("endTime");
    public static final CString STATE = new CString("state");
    public static final CString START_TASK = new CString("startTask");
    public static final CString RETURN_VALUE = new CString("returnValue");

    private TaskContainerBSerializer taskContainerBSerializer = new TaskContainerBSerializer();

    @Override
    public Class<Process> getObjectClass() {
        return Process.class;
    }

    @Override
    public void write(BDataOutput out, Process object) {
        out.writeUUID(_ID, object.getProcessId());

        out.writeString(CUSTOM_ID, object.getCustomId());
        out.writeLong(START_TIME, object.getStartTime(), -1l);
        out.writeLong(END_TIME, object.getEndTime(), -1l);
        out.writeInt(STATE, object.getState(), 0);
        out.writeString(RETURN_VALUE, object.getReturnValue());

        int startTaskLabel = out.writeObject(START_TASK);
        taskContainerBSerializer.write(out, object.getStartTask());
        out.writeObjectStop(startTaskLabel);
    }

    @Override
    public Process read(BDataInput in) {
        String customId = in.readString(CUSTOM_ID);
        long startTime = in.readLong(START_TIME, -1l);
        long endTime = in.readLong(END_TIME, -1l);
        int state = in.readInt(STATE, 0);
        String returnValue = in.readString(RETURN_VALUE);

        int readObject = in.readObject(START_TASK);
        TaskContainer startTask = taskContainerBSerializer.read(in);
        in.readObjectStop(readObject);

        return new Process(startTask, customId, startTime, endTime, state, returnValue);
    }
}

package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * Created by greg on 04/02/15.
 */
public class ProcessSerializer implements StreamBSerializer<Process> {

    public static final CString PROCESS_ID = new CString("pId");
    public static final CString START_TASK_ID = new CString("sTaId");
    public static final CString CUSTOM_ID = new CString("cId");
    public static final CString START_TIME = new CString("sTime");
    public static final CString END_TIME = new CString("eTime");
    public static final CString STATE = new CString("state");
    public static final CString START_TASK = new CString("sTask");
    public static final CString RETURN_VALUE = new CString("retVal");
    private TaskContainerSerializer taskContainerSerializer = new TaskContainerSerializer();

    @Override
    public Class<Process> getObjectClass() {
        return Process.class;
    }

    @Override
    public void write(BDataOutput out, Process object) {
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeUUID(START_TASK_ID, object.getStartTaskId());
        out.writeString(CUSTOM_ID, object.getCustomId());
        out.writeLong(START_TIME, object.getStartTime());
        out.writeLong(END_TIME, object.getEndTime());
        out.writeInt(STATE, object.getState());
        out.writeString(RETURN_VALUE, object.getReturnValue());
        int startTaskLabel = out.writeObject(START_TASK);
        taskContainerSerializer.write(out, object.getStartTask());
        out.writeObjectStop(startTaskLabel);
    }

    @Override
    public Process read(BDataInput in) {
        UUID processId = in.readUUID(PROCESS_ID);
        UUID startTaskId = in.readUUID(START_TASK_ID);
        String customId = in.readString(CUSTOM_ID);
        long startTime = in.readLong(START_TIME);
        long endTime = in.readLong(END_TIME);
        int state = in.readInt(STATE);
        String returnValue = in.readString(RETURN_VALUE);
        int readObject = in.readObject(START_TASK);
        TaskContainer startTask = null;
        if (readObject != -1) {
            startTask = taskContainerSerializer.read(in);
            in.readObjectStop(readObject);
        }

        final Process process = new Process();
        process.setProcessId(processId);
        process.setStartTaskId(startTaskId);
        process.setCustomId(customId);
        process.setStartTime(startTime);
        process.setEndTime(endTime);
        process.setState(state);
        process.setReturnValue(returnValue);
        process.setStartTask(startTask);
        return process;
    }
}

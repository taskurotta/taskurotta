package ru.taskurotta.service.hz.serialization.bson;


import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import java.util.UUID;


/**
 * Created by greg on 03/02/15.
 */
public class TaskContainerSerializer implements StreamBSerializer<TaskContainer> {
    private static final CString TASK_ID = new CString("taskId");
    private static final CString PROCESS_ID = new CString("processId");
    private static final CString ACTOR_ID = new CString("actorId");
    private static final CString METHOD = new CString("method");
    private static final CString ERROR_ATTEMPTS = new CString("errorAttempts");
    private static final CString TYPE = new CString("type");
    private static final CString START_TIME = new CString("startTime");
    private static final CString FAIL_TYPES = new CString("failTypes");
    private static final CString ARG_CONTAINERS = new CString("argContainers");
    private static final CString UNSAFE = new CString("unsafe");
    public static final CString TASK_OPTIONS_CONTAINER = new CString("taskOptionsContainer");

    private ArgContainerSerializer argContainerSerializer = new ArgContainerSerializer();
    private TaskOptionsContainerSerializer taskOptionsContainerSerializer = new TaskOptionsContainerSerializer();

    @Override
    public Class<TaskContainer> getObjectClass() {
        return TaskContainer.class;
    }

    @Override
    public void write(BDataOutput out, TaskContainer object) {
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeString(ACTOR_ID, object.getActorId());
        out.writeString(METHOD, object.getMethod());
        out.writeInt(ERROR_ATTEMPTS, object.getErrorAttempts());
        out.writeLong(START_TIME, object.getStartTime());
        out.writeInt(TYPE, object.getType().getValue());
        out.writeInt(UNSAFE, (object.isUnsafe() ? 1 : 0));
        if (object.getFailTypes() != null) {
            int failTypesLabel = out.writeArray(FAIL_TYPES);
            for (int i = 0; i < object.getFailTypes().length; i++) {
                String s = object.getFailTypes()[i];
                out.writeString(i, s);
            }
            out.writeArrayStop(failTypesLabel);
        }
        argContainerSerializer.writeArgContainersArray(ARG_CONTAINERS, out, object.getArgs());
        if (object.getOptions() != null) {
            int taskOptionsContainerLabel = out.writeObject(TASK_OPTIONS_CONTAINER);
            taskOptionsContainerSerializer.write(out, object.getOptions());
            out.writeObjectStop(taskOptionsContainerLabel);
        }
    }

    @Override
    public TaskContainer read(BDataInput in) {
        UUID taskId = in.readUUID(TASK_ID);
        UUID processId = in.readUUID(PROCESS_ID);
        String actorId = in.readString(ACTOR_ID);
        String method = in.readString(METHOD);
        int errorAttempts = in.readInt(ERROR_ATTEMPTS);
        long startTime = in.readLong(START_TIME);
        int type = in.readInt(TYPE);
        boolean unsafe = in.readInt(UNSAFE) == 1;
        int failTypesLabel = in.readArray(FAIL_TYPES);
        String[] failTypes = null;
        if (failTypesLabel>0) {
            int failTypesSize = in.readArraySize();
            failTypes = new String[failTypesSize];
            for (int i = 0; i < failTypesSize; i++) {
                failTypes[i] = in.readString(i);
            }
            in.readArrayStop(failTypesLabel);
        }
        ArgContainer[] argContainers = argContainerSerializer.readArgContainersArray(ARG_CONTAINERS, in);

        int taskOptionsContainerLabel = in.readObject(TASK_OPTIONS_CONTAINER);
        TaskOptionsContainer taskOptionsContainer = null;
        if (taskOptionsContainerLabel != -1) {
            taskOptionsContainer = taskOptionsContainerSerializer.read(in);
            in.readObjectStop(taskOptionsContainerLabel);
        }

        return new TaskContainer(
                taskId,
                processId,
                method,
                actorId,
                TaskType.fromInt(type),
                startTime,
                errorAttempts, argContainers, taskOptionsContainer, unsafe, failTypes);
    }
}

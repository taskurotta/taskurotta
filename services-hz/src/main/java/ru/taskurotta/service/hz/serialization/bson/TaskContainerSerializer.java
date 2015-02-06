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

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readArrayOfString;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeArrayOfString;


/**
 * Created by greg on 03/02/15.
 */
public class TaskContainerSerializer implements StreamBSerializer<TaskContainer> {
    private static final CString TASK_ID = new CString("taskId");
    private static final CString PROCESS_ID = new CString("processId");
    private static final CString ACTOR_ID = new CString("aId");
    private static final CString METHOD = new CString("me");
    private static final CString ERROR_ATTEMPTS = new CString("eAtt");
    private static final CString TYPE = new CString("ty");
    private static final CString START_TIME = new CString("sTi");
    private static final CString FAIL_TYPES = new CString("fTy");
    private static final CString ARG_CONTAINERS = new CString("aCon");
    private static final CString UNSAFE = new CString("uSa");
    private static final CString TASK_OPTIONS_CONTAINER = new CString("tOptCon");

    private ArgContainerSerializer argContainerSerializer = new ArgContainerSerializer();
    private TaskOptionsContainerSerializer taskOptionsContainerSerializer = new TaskOptionsContainerSerializer();

    @Override
    public Class<TaskContainer> getObjectClass() {
        return TaskContainer.class;
    }

    @Override
    public void write(BDataOutput out, TaskContainer object) {
        int idLabel = out.writeObject(_ID);
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeObjectStop(idLabel);

        out.writeString(METHOD, object.getMethod());
        out.writeString(ACTOR_ID, object.getActorId());
        out.writeInt(TYPE, object.getType().getValue());
        out.writeLong(START_TIME, object.getStartTime());
        out.writeInt(ERROR_ATTEMPTS, object.getErrorAttempts());
        argContainerSerializer.writeArgContainersArray(ARG_CONTAINERS, out, object.getArgs());
        if (object.getOptions() != null) {
            int taskOptionsContainerLabel = out.writeObject(TASK_OPTIONS_CONTAINER);
            taskOptionsContainerSerializer.write(out, object.getOptions());
            out.writeObjectStop(taskOptionsContainerLabel);
        }
        out.writeInt(UNSAFE, (object.isUnsafe() ? 1 : 0));
        writeArrayOfString(FAIL_TYPES, object.getFailTypes(), out);
    }

    @Override
    public TaskContainer read(BDataInput in) {
        int idLabel = in.readObject(_ID);
        UUID processId = in.readUUID(PROCESS_ID);
        UUID taskId = in.readUUID(TASK_ID);
        in.readObjectStop(idLabel);
        String method = in.readString(METHOD);
        String actorId = in.readString(ACTOR_ID);
        TaskType type = TaskType.fromInt(in.readInt(TYPE));
        long startTime = in.readLong(START_TIME);
        int errorAttempts = in.readInt(ERROR_ATTEMPTS);
        ArgContainer[] argContainers = argContainerSerializer.readArgContainersArray(ARG_CONTAINERS, in);
        int taskOptionsContainerLabel = in.readObject(TASK_OPTIONS_CONTAINER);
        TaskOptionsContainer taskOptionsContainer = null;
        if (taskOptionsContainerLabel != -1) {
            taskOptionsContainer = taskOptionsContainerSerializer.read(in);
            in.readObjectStop(taskOptionsContainerLabel);
        }
        boolean unsafe = in.readInt(UNSAFE) == 1;
        String[] failTypes = readArrayOfString(FAIL_TYPES, in);
        return new TaskContainer(
                taskId,
                processId,
                method,
                actorId,
                type,
                startTime,
                errorAttempts, argContainers, taskOptionsContainer, unsafe, failTypes);
    }
}

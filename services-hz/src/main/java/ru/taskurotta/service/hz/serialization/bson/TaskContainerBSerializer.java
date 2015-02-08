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

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readArrayOfObjects;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readArrayOfString;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readObject;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeArrayOfObjects;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeArrayOfString;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeObjectIfNotNull;


public class TaskContainerBSerializer implements StreamBSerializer<TaskContainer> {

    private static final CString TASK_ID = new CString("t");
    private static final CString PROCESS_ID = new CString("p");
    private static final CString ACTOR_ID = new CString("actorId");
    private static final CString METHOD = new CString("method");
    private static final CString ERROR_ATTEMPTS = new CString("errorAttempts");
    private static final CString TYPE = new CString("type");
    private static final CString START_TIME = new CString("startTime");
    private static final CString FAIL_TYPES = new CString("failTypes");
    private static final CString ARGS = new CString("args");
    private static final CString UNSAFE = new CString("unsafe");
    private static final CString TASK_OPTIONS = new CString("options");

    protected static BSerializerTools.ArrayFactory<TaskContainer> arrayFactory =
            new BSerializerTools.ArrayFactory<TaskContainer>() {
        @Override
        public TaskContainer[] create(int size) {
            return new TaskContainer[size];
        }
    };

    private ArgContainerBSerializer argContainerBSerializer = new ArgContainerBSerializer();
    private TaskOptionsContainerBSerializer taskOptionsContainerBSerializer = new TaskOptionsContainerBSerializer();

    @Override
    public Class<TaskContainer> getObjectClass() {
        return TaskContainer.class;
    }

    @Override
    public void write(BDataOutput out, TaskContainer object) {
        int writeIdLabel = out.writeObject(_ID);
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeObjectStop(writeIdLabel);

        out.writeString(METHOD, object.getMethod());
        out.writeString(ACTOR_ID, object.getActorId());
        out.writeInt(TYPE, object.getType().getValue());
        out.writeLong(START_TIME, object.getStartTime());
        out.writeInt(ERROR_ATTEMPTS, object.getErrorAttempts());
        writeArrayOfObjects(ARGS, object.getArgs(), argContainerBSerializer, out);
        writeObjectIfNotNull(TASK_OPTIONS, object.getOptions(), taskOptionsContainerBSerializer, out);
        out.writeBoolean(UNSAFE, object.isUnsafe());
        writeArrayOfString(FAIL_TYPES, object.getFailTypes(), out);
    }

    @Override
    public TaskContainer read(BDataInput in) {
        int readIdLabel = in.readObject(_ID);
        UUID taskId = in.readUUID(TASK_ID);
        UUID processId = in.readUUID(PROCESS_ID);
        in.readObjectStop(readIdLabel);

        String method = in.readString(METHOD);
        String actorId = in.readString(ACTOR_ID);
        TaskType type = TaskType.fromInt(in.readInt(TYPE));
        long startTime = in.readLong(START_TIME);
        int errorAttempts = in.readInt(ERROR_ATTEMPTS);
        ArgContainer[] argContainers = readArrayOfObjects(ARGS, ArgContainerBSerializer.arrayFactory,
                argContainerBSerializer, in);
        TaskOptionsContainer taskOptionsContainer = readObject(TASK_OPTIONS,
                taskOptionsContainerBSerializer, in);
        boolean unsafe = in.readBoolean(UNSAFE);
        String[] failTypes = readArrayOfString(FAIL_TYPES, in);

        return new TaskContainer(taskId, processId, method, actorId, type, startTime, errorAttempts, argContainers,
                taskOptionsContainer, unsafe, failTypes);
    }
}

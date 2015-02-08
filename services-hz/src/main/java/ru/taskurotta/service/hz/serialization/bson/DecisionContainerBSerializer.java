package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readArrayOfObjects;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readObject;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeArrayOfObjects;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeObjectIfNotNull;

public class DecisionContainerBSerializer implements StreamBSerializer<DecisionContainer> {

    public static final CString TASK_ID = new CString("t");
    public static final CString PROCESS_ID = new CString("p");
    public static final CString ACTOR_ID = new CString("actorId");
    public static final CString VALUE = new CString("value");
    public static final CString ERROR = new CString("errorContainer");
    public static final CString RESTART_TIME = new CString("restartTime");
    public static final CString EXECUTION_TIME = new CString("executionTime");
    public static final CString TASKS = new CString("tasks");

    private ArgContainerBSerializer argContainerBSerializer = new ArgContainerBSerializer();
    private ErrorContainerBSerializer errorContainerBSerializer = new ErrorContainerBSerializer();
    private TaskContainerBSerializer taskContainerBSerializer = new TaskContainerBSerializer();

    @Override
    public Class<DecisionContainer> getObjectClass() {
        return DecisionContainer.class;
    }

    @Override
    public void write(BDataOutput out, DecisionContainer object) {

        int writeIdLabel = out.writeObject(_ID);
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeObjectStop(writeIdLabel);

        writeObjectIfNotNull(VALUE, object.getValue(), argContainerBSerializer, out);
        writeObjectIfNotNull(ERROR, object.getErrorContainer(), errorContainerBSerializer, out);

        out.writeLong(EXECUTION_TIME, object.getExecutionTime());
        out.writeLong(RESTART_TIME, object.getRestartTime());
        out.writeString(ACTOR_ID, object.getActorId());

        writeArrayOfObjects(TASKS, object.getTasks(), taskContainerBSerializer, out);
    }

    @Override
    public DecisionContainer read(BDataInput in) {

        int readIdLabel = in.readObject(_ID);
        UUID taskId = in.readUUID(TASK_ID);
        UUID processId = in.readUUID(PROCESS_ID);
        in.readObjectStop(readIdLabel);

        ArgContainer value = readObject(VALUE, argContainerBSerializer, in);
        ErrorContainer errorContainer = readObject(ERROR, errorContainerBSerializer, in);

        long restartTime = in.readLong(RESTART_TIME);
        long executionTime = in.readLong(EXECUTION_TIME);
        String actorId = in.readString(ACTOR_ID);

        TaskContainer[] tasks = readArrayOfObjects(TASKS, TaskContainerBSerializer.arrayFactory,
                taskContainerBSerializer, in);

        return new DecisionContainer(taskId, processId, value, errorContainer, restartTime, tasks, actorId,
                executionTime);
    }
}

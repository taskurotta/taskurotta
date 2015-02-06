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

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readObject;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeObjectIfNotNull;

/**
 * Created by greg on 04/02/15.
 */
public class DecisionContainerSerializer implements StreamBSerializer<DecisionContainer> {

    public static final CString PROCESS_ID = new CString("processId");
    public static final CString TASK_ID = new CString("taskId");
    public static final CString ACTOR_ID = new CString("aId");
    public static final CString VALUE = new CString("val");
    public static final CString ERROR_CONTAINER = new CString("eCont");
    public static final CString RESTART_TIME = new CString("rTime");
    public static final CString EXECUTION_TIME = new CString("eTime");
    public static final CString TASKS = new CString("tsks");

    private ArgContainerSerializer argContainerSerializer = new ArgContainerSerializer();
    private ErrorContainerSerializer errorContainerSerializer = new ErrorContainerSerializer();
    private TaskContainerSerializer taskContainerSerializer = new TaskContainerSerializer();

    @Override

    public Class<DecisionContainer> getObjectClass() {
        return DecisionContainer.class;
    }

    @Override
    public void write(BDataOutput out, DecisionContainer object) {
        int writeIdLabel = out.writeObject(_ID);
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeObjectStop(writeIdLabel);

        writeObjectIfNotNull(VALUE, object.getValue(), argContainerSerializer, out);
        writeObjectIfNotNull(ERROR_CONTAINER, object.getErrorContainer(), errorContainerSerializer, out);
        out.writeLong(RESTART_TIME, object.getRestartTime());
        out.writeLong(EXECUTION_TIME, object.getExecutionTime());
        out.writeString(ACTOR_ID, object.getActorId());
        if (object.getTasks() != null) {
            int tasksLabel = out.writeArray(TASKS);
            for (int i = 0; i < object.getTasks().length; i++) {
                TaskContainer taskContainer = object.getTasks()[i];
                writeObjectIfNotNull(i, taskContainer, taskContainerSerializer, out);
            }
            out.writeArrayStop(tasksLabel);
        }
    }

    @Override
    public DecisionContainer read(BDataInput in) {
        int readIdLabel = in.readObject(_ID);
        UUID processId = in.readUUID(PROCESS_ID);
        UUID taskId = in.readUUID(TASK_ID);
        in.readObjectStop(readIdLabel);

        ArgContainer value = readObject(VALUE, argContainerSerializer, in);
        ErrorContainer errorContainer = readObject(ERROR_CONTAINER, errorContainerSerializer, in);
        long restartTime = in.readLong(RESTART_TIME);
        long executionTime = in.readLong(EXECUTION_TIME);
        String actorId = in.readString(ACTOR_ID);
        TaskContainer[] tasks = null;
        int tasksLabel = in.readArray(TASKS);
        if (tasksLabel != -1) {
            int arraySize = in.readArraySize();
            tasks = new TaskContainer[arraySize];
            for (int i = 0; i < arraySize; i++) {
                tasks[i] = readObject(i, taskContainerSerializer, in);
            }
            in.readArrayStop(tasksLabel);
        }

        return new DecisionContainer(
                taskId,
                processId,
                value,
                errorContainer,
                restartTime,
                tasks,
                actorId,
                executionTime);
    }
}

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

/**
 * Created by greg on 04/02/15.
 */
public class DecisionContainerSerializer implements StreamBSerializer<DecisionContainer> {

    public static final CString PROCESS_ID = new CString("pId");
    public static final CString TASK_ID = new CString("tId");
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
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeString(ACTOR_ID, object.getActorId());
        int valueLabel = out.writeObject(VALUE);
        argContainerSerializer.write(out, object.getValue());
        out.writeObjectStop(valueLabel);
        int errorLabel = out.writeObject(ERROR_CONTAINER);
        errorContainerSerializer.write(out, object.getErrorContainer());
        out.writeObjectStop(errorLabel);
        out.writeLong(RESTART_TIME, object.getRestartTime());
        out.writeLong(EXECUTION_TIME, object.getExecutionTime());
        int tasksLabel = out.writeArray(TASKS);
        for (int i = 0; i < object.getTasks().length; i++) {
            TaskContainer taskContainer = object.getTasks()[i];
            int objLabel = out.writeObject(new CString(Integer.toString(i)));
            taskContainerSerializer.write(out, taskContainer);
            out.writeObjectStop(objLabel);
        }
        out.writeArrayStop(tasksLabel);
    }

    @Override
    public DecisionContainer read(BDataInput in) {
        UUID processId = in.readUUID(PROCESS_ID);
        UUID taskId = in.readUUID(TASK_ID);
        String actorId = in.readString(ACTOR_ID);
        int valueLabel = in.readObject(VALUE);
        ArgContainer value = null;
        if (valueLabel != -1) {
            value = argContainerSerializer.read(in);
            in.readObjectStop(valueLabel);
        }
        int errorLabel = in.readObject(ERROR_CONTAINER);
        ErrorContainer errorContainer = null;
        if (errorLabel != -1) {
            errorContainer = errorContainerSerializer.read(in);
            in.readObjectStop(errorLabel);
        }
        long restartTime = in.readLong(RESTART_TIME);
        long executionTime = in.readLong(EXECUTION_TIME);
        TaskContainer[] tasks = null;
        int tasksLabel = in.readArray(TASKS);
        if (tasksLabel != -1) {
            int arraySize = in.readArraySize();
            tasks = new TaskContainer[arraySize];
            for (int i = 0; i < arraySize; i++) {
                int objLabel = in.readObject(new CString(Integer.toString(i)));
                tasks[i] = taskContainerSerializer.read(in);
                in.readObjectStop(objLabel);

            }
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

package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

/**
 * Created by greg on 03/02/15.
 */
public class TaskOptionsContainerSerializer implements StreamBSerializer<TaskOptionsContainer> {

    private static final CString ARG_TYPES = new CString("argTypes");
    private static final CString ARG_CONTAINERS = new CString("argContainers");
    private static final CString TASK_CONFIG_CONTAINER = new CString("taskConfigContainer");
    private ArgContainerSerializer argContainerSerializer = new ArgContainerSerializer();

    private TaskConfigContainerSerializer taskConfigContainerSerializer = new TaskConfigContainerSerializer();

    @Override
    public Class<TaskOptionsContainer> getObjectClass() {
        return TaskOptionsContainer.class;
    }

    @Override
    public void write(BDataOutput out, TaskOptionsContainer object) {
        if (object.getTaskConfigContainer() != null) {
            int taskConfigContainerLabel = out.writeObject(TASK_CONFIG_CONTAINER);
            taskConfigContainerSerializer.write(out, object.getTaskConfigContainer());
            out.writeObjectStop(taskConfigContainerLabel);
        }
        if (object.getArgTypes() != null) {
            int argTypesLabel = out.writeArray(ARG_TYPES);
            for (int i = 0; i < object.getArgTypes().length; i++) {
                ArgType argType = object.getArgTypes()[i];
                if (argType != null) {
                    out.writeInt(i, argType.getValue());
                } else {
                    out.writeInt(i, -1);
                }
            }
            out.writeArrayStop(argTypesLabel);
        }
        argContainerSerializer.writeArgContainersArray(ARG_CONTAINERS, out, object.getPromisesWaitFor());

    }

    @Override
    public TaskOptionsContainer read(BDataInput in) {
        TaskConfigContainer taskConfigContainer = null;
        int taskConfigContainerLabel = in.readObject(TASK_CONFIG_CONTAINER);
        if (taskConfigContainerLabel != -1) {
            taskConfigContainer = taskConfigContainerSerializer.read(in);
            in.readObjectStop(taskConfigContainerLabel);
        }
        int argTypesLabel = in.readArray(ARG_TYPES);
        ArgType[] argTypes = null;
        if (argTypesLabel != -1) {
            int argTypesSize = in.readArraySize();
            argTypes = new ArgType[argTypesSize];
            for (int i = 0; i < argTypesSize; i++) {
                int id = in.readInt(i, -1);
                argTypes[i] = (id == -1) ? null : ArgType.fromInt(id);
            }
            in.readArrayStop(argTypesLabel);
        }
        ArgContainer[] argContainers = argContainerSerializer.readArgContainersArray(ARG_CONTAINERS, in);
        return new TaskOptionsContainer(argTypes, taskConfigContainer, argContainers);
    }
}

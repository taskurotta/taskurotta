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
        int argTypesLabel = out.writeArray(ARG_TYPES);
        for (int i = 0; i < object.getArgTypes().length; i++) {
            ArgType argType = object.getArgTypes()[i];
            out.writeLong(i, argType.getValue());
        }
        out.writeArrayStop(argTypesLabel);
        int argContainersLabel = out.writeArray(ARG_CONTAINERS);
        for (int i = 0; i < object.getPromisesWaitFor().length; i++) {
            int writeObjectLabel = out.writeObject(SerializerTools.createCString(i));
            ArgContainer argContainer = object.getPromisesWaitFor()[i];
            argContainerSerializer.write(out, argContainer);
            out.writeObjectStop(writeObjectLabel);
        }
        out.writeArrayStop(argContainersLabel);
        int taskConfigContainerLabel = out.writeObject(TASK_CONFIG_CONTAINER);
        taskConfigContainerSerializer.write(out, object.getTaskConfigContainer());
        out.writeObjectStop(taskConfigContainerLabel);
    }

    @Override
    public TaskOptionsContainer read(BDataInput in) {
        int argTypesLabel = in.readArray(ARG_TYPES);
        ArgType[] argTypes = null;
        if (argTypesLabel != -1) {
            int argTypesSize = in.readArraySize();
            if (argTypesSize > 0) {
                argTypes = new ArgType[argTypesSize];
                for (int i = 0; i < argTypesSize; i++) {
                    argTypes[i] = ArgType.fromInt((int) in.readLong(i));
                }
            }
            in.readArrayStop(argTypesLabel);
        }
        ArgContainer[] argContainers = null;
        int argContainersLabel = in.readArray(ARG_CONTAINERS);
        int argContainersSize = in.readArraySize();
        if (argContainersLabel != -1) {
            if (argContainersSize > 0) {
                argContainers = new ArgContainer[argContainersSize];
                for (int i = 0; i < argContainersSize; i++) {
                    int objectLabel = in.readObject(new CString(i));
                    argContainers[i] = argContainerSerializer.read(in);
                    in.readObjectStop(objectLabel);
                }
            }
            in.readArrayStop(argContainersLabel);
        }
        int taskConfigContainerLabel = in.readObject(TASK_CONFIG_CONTAINER);
        TaskConfigContainer taskConfigContainer = null;
        if (taskConfigContainerLabel != -1) {
            taskConfigContainer = taskConfigContainerSerializer.read(in);
            in.readObjectStop(taskConfigContainerLabel);
        }
        return new TaskOptionsContainer(argTypes, taskConfigContainer, argContainers);
    }
}

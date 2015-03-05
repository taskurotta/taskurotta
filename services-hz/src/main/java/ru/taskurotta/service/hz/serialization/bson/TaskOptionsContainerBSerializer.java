package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readArrayOfObjects;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readObject;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeArrayOfObjects;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeObjectIfNotNull;

/**
 * Created by greg on 03/02/15.
 */
public class TaskOptionsContainerBSerializer implements StreamBSerializer<TaskOptionsContainer> {

    private static final CString ARG_TYPES = new CString("argTypes");
    private static final CString WAIT_FOR = new CString("waitFor");
    private static final CString TASK_CONFIG = new CString("taskConfig");

    private ArgContainerBSerializer argContainerBSerializer = new ArgContainerBSerializer();
    private TaskConfigContainerBSerializer taskConfigContainerBSerializer = new TaskConfigContainerBSerializer();

    @Override
    public Class<TaskOptionsContainer> getObjectClass() {
        return TaskOptionsContainer.class;
    }

    @Override
    public void write(BDataOutput out, TaskOptionsContainer object) {

        writeObjectIfNotNull(TASK_CONFIG, object.getTaskConfigContainer(), taskConfigContainerBSerializer,
                out);

        ArgType[] argTypes = object.getArgTypes();
        if (argTypes != null) {
            int argTypesLabel = out.writeArray(ARG_TYPES);
            for (int i = 0; i < argTypes.length; i++) {
                ArgType argType = argTypes[i];
                if (argType != null) {
                    out.writeInt(i, argType.getValue());
                } else {
                    out.writeInt(i, -1);
                }
            }
            out.writeArrayStop(argTypesLabel);
        }

        writeArrayOfObjects(WAIT_FOR, object.getPromisesWaitFor(), argContainerBSerializer, out);
    }

    @Override
    public TaskOptionsContainer read(BDataInput in) {
        TaskConfigContainer taskConfigContainer = readObject(TASK_CONFIG, taskConfigContainerBSerializer, in);

        int argTypesLabel = in.readArray(ARG_TYPES);
        ArgType[] argTypes = null;
        if (argTypesLabel != -1) {
            int argTypesSize = in.readArraySize();
            argTypes = new ArgType[argTypesSize];
            for (int i = 0; i < argTypesSize; i++) {
                int id = in.readInt(i);
                argTypes[i] = (id == -1) ? null : ArgType.fromInt(id);
            }
            in.readArrayStop(argTypesLabel);
        }

        ArgContainer[] argContainers = readArrayOfObjects(WAIT_FOR, ArgContainerBSerializer.arrayFactory,
                argContainerBSerializer, in);

        return new TaskOptionsContainer(argTypes, taskConfigContainer, argContainers);
    }
}

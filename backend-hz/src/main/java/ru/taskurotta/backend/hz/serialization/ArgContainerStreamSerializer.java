package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.ArgContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.taskurotta.backend.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.backend.hz.serialization.SerializationTools.writeString;

/**
 * User: greg
 */
public class ArgContainerStreamSerializer implements StreamSerializer<ArgContainer> {

    @Override
    public void write(ObjectDataOutput out, ArgContainer argContainer) throws IOException {

        if (argContainer == null) {
            out.writeBoolean(false);
            return;
        }

        out.writeBoolean(true);
        serializePlain(out, argContainer);
        compositeWrite(out, argContainer);
    }

    private void compositeWrite(ObjectDataOutput out, ArgContainer argContainer) throws IOException {
        if (argContainer.getCompositeValue() != null && argContainer.getCompositeValue().length > 0) {
            out.writeInt(argContainer.getCompositeValue().length);
            for (ArgContainer arg : argContainer.getCompositeValue()) {
                serializePlain(out, arg);
            }
        } else {
            out.writeInt(-1);
        }
    }

    @Override
    public ArgContainer read(ObjectDataInput in) throws IOException {

        if (!in.readBoolean()) {
            return null;
        }

        ArgContainer arg;
        arg = deserializePlain(in);
        return compositeRead(in, arg);
    }

    private void serializePlain(ObjectDataOutput out, ArgContainer argContainer) throws IOException {
        writeString(out, argContainer.getClassName());
        if (argContainer.getTaskId() == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            UUIDSerializer.write(out, argContainer.getTaskId());
        }
        out.writeBoolean(argContainer.isReady());
        writeString(out, argContainer.getJSONValue());
        if (argContainer.getType() != null) {
            out.writeInt(argContainer.getType().getValue());
        } else {
            out.writeInt(-1);
        }
        out.writeBoolean(argContainer.isPromise());
    }

    private ArgContainer deserializePlain(ObjectDataInput in) throws IOException {
        ArgContainer arg = new ArgContainer();
        String className = readString(in);
        UUID taskId = null;
        boolean taskIdSetted = in.readBoolean();
        if (taskIdSetted) {
            taskId = UUIDSerializer.read(in);
        }
        boolean ready = in.readBoolean();
        String jsonValue = readString(in);
        int type = in.readInt();
        ArgContainer.ValueType valueType = null;
        if (type != -1) {
            valueType = ArgContainer.ValueType.fromInt(type);
        }
        boolean promise = in.readBoolean();
        arg.setClassName(className);
        arg.setTaskId(taskId);
        arg.setReady(ready);
        arg.setJSONValue(jsonValue);
        arg.setType(valueType);
        arg.setPromise(promise);
        return arg;
    }

    private ArgContainer compositeRead(ObjectDataInput in, ArgContainer arg) throws IOException {
        int compositeSize = in.readInt();
        if (compositeSize != -1) {
            List<ArgContainer> containerList = new ArrayList<>();
            for (int i = 0; i < compositeSize; i++) {
                ArgContainer argComposite = deserializePlain(in);
                containerList.add(argComposite);
            }
            ArgContainer[] compositeValues = new ArgContainer[containerList.size()];
            containerList.toArray(compositeValues);
            arg.setCompositeValue(compositeValues);
        }
        return arg;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.ARG_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

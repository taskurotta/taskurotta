package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.ArgContainer;

import java.io.IOException;
import java.util.UUID;

import static ru.taskurotta.service.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeString;

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

        ArgContainer[] compositeValue = argContainer.getCompositeValue();

        if (compositeValue == null) {
            out.writeInt(-1);
            return;
        }

        if (compositeValue.length == 0) {
            out.writeInt(0);
            return;
        }

        out.writeInt(argContainer.getCompositeValue().length);
        for (ArgContainer arg : argContainer.getCompositeValue()) {
            serializePlain(out, arg);
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
        writeString(out, argContainer.getDataType());
        if (argContainer.getTaskId() == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            UUIDSerializer.write(out, argContainer.getTaskId());
        }
        out.writeBoolean(argContainer.isReady());
        writeString(out, argContainer.getJSONValue());
        if (argContainer.getValueType() != null) {
            out.writeInt(argContainer.getValueType().getValue());
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
        arg.setDataType(className);
        arg.setTaskId(taskId);
        arg.setReady(ready);
        arg.setJSONValue(jsonValue);
        arg.setValueType(valueType);
        arg.setPromise(promise);
        return arg;
    }

    private ArgContainer compositeRead(ObjectDataInput in, ArgContainer arg) throws IOException {
        int compositeSize = in.readInt();

        switch (compositeSize) {

            case -1:
                arg.setCompositeValue(null);
                break;

            case 0:
                arg.setCompositeValue(new ArgContainer[0]);
                break;

            default:
                ArgContainer[] compositeValues = new ArgContainer[compositeSize];
                for (int i = 0; i < compositeSize; i++) {
                    compositeValues[i] = deserializePlain(in);
                }
                arg.setCompositeValue(compositeValues);
                break;

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

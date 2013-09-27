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
public class ArgContainerSerializer implements StreamSerializer<ArgContainer> {

    @Override
    public void write(ObjectDataOutput out, ArgContainer argContainer) throws IOException {
        serializePlain(out, argContainer);
        if (argContainer.getCompositeValue() != null && argContainer.getCompositeValue().length > 0) {
            out.writeInt(argContainer.getCompositeValue().length);
            for (ArgContainer arg : argContainer.getCompositeValue()) {
                write(out, arg);
            }
        } else {
            out.writeInt(-1);
        }
    }

    private void serializePlain(ObjectDataOutput out, ArgContainer argContainer) throws IOException {
        writeString(out, argContainer.getClassName());
        UUIDSerializer.write(out, argContainer.getTaskId());
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
        UUID taskId = UUIDSerializer.read(in);
        boolean ready = in.readBoolean();
        String jsonValue = readString(in);
        int type = in.readInt();
        ArgContainer.ValueType valueType = null;
        if (type != -1) {
            switch (type) {
                case 0:
                    valueType = ArgContainer.ValueType.PLAIN;
                    break;
                case 1:
                    valueType = ArgContainer.ValueType.ARRAY;
                    break;
                case 2:
                    valueType = ArgContainer.ValueType.COLLECTION;
                    break;
            }

        }
        boolean promise = in.readBoolean();

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

        arg.setClassName(className);
        arg.setTaskId(taskId);
        arg.setReady(ready);
        arg.setJSONValue(jsonValue);
        arg.setType(valueType);
        arg.setPromise(promise);
        return arg;
    }

    @Override
    public ArgContainer read(ObjectDataInput in) throws IOException {
        ArgContainer arg;
        arg = deserializePlain(in);
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

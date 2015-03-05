package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ErrorContainer;

import java.util.UUID;

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeObjectIfNotNull;

public class ArgContainerBSerializer implements StreamBSerializer<ArgContainer> {

    private final CString TASK_ID = new CString("t");
    private final CString DATA_TYPE = new CString("dataType");
    private final CString IS_READY = new CString("ready");
    private final CString JSON_VALUE = new CString("JSONValue");
    private final CString ERROR = new CString("errorContainer");
    private final CString VALUE_TYPE = new CString("valueType");
    private final CString IS_PROMISE = new CString("promise");
    private final CString COMPOSITE_VALUE = new CString("compositeType");

    protected static BSerializerTools.ArrayFactory<ArgContainer> arrayFactory = new BSerializerTools.ArrayFactory<ArgContainer>() {
        @Override
        public ArgContainer[] create(int size) {
            return new ArgContainer[size];
        }
    };

    private ErrorContainerBSerializer errorContainerBSerializer = new ErrorContainerBSerializer();

    @Override
    public Class<ArgContainer> getObjectClass() {
        return ArgContainer.class;
    }

    @Override
    public void write(BDataOutput out, ArgContainer object) {
        serializePlain(out, object);
        compositeWrite(out, object);
    }

    private void serializePlain(BDataOutput out, ArgContainer object) {
        out.writeString(DATA_TYPE, object.getDataType());
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeBoolean(IS_READY, object.isReady(), true);
        out.writeString(JSON_VALUE, object.getJSONValue());
        writeObjectIfNotNull(ERROR, object.getErrorContainer(), errorContainerBSerializer, out);
        if (object.getValueType() != null) {
            out.writeInt(VALUE_TYPE, object.getValueType().getValue());
        }

        out.writeBoolean(IS_PROMISE, object.isPromise(), false);
    }

    private void compositeWrite(BDataOutput out, ArgContainer object) {
        ArgContainer[] compositeValue = object.getCompositeValue();

        if (compositeValue == null) {
            return;
        }

        int label = out.writeArray(COMPOSITE_VALUE);
        for (int i = 0; i < compositeValue.length; i++) {
            int objectLabel = out.writeObject(i);
            serializePlain(out, compositeValue[i]);
            out.writeObjectStop(objectLabel);

        }
        out.writeArrayStop(label);
    }

    @Override
    public ArgContainer read(BDataInput in) {

        ArgContainer arg = deserializePlain(in);
        return compositeRead(in, arg);
    }

    private ArgContainer deserializePlain(BDataInput in) {

        String dataType = in.readString(DATA_TYPE);
        UUID taskId = in.readUUID(TASK_ID);
        boolean isReady = in.readBoolean(IS_READY, true);
        String value = in.readString(JSON_VALUE);
        ArgContainer.ValueType valueType = null;
        int valueTypeInt = in.readInt(VALUE_TYPE, -1);
        if (valueTypeInt != -1) {
            valueType = ArgContainer.ValueType.fromInt(valueTypeInt);
        }
        ErrorContainer errorContainer = null;
        int errorContainerLabel = in.readObject(ERROR);
        if (errorContainerLabel != -1) {
            errorContainer = errorContainerBSerializer.read(in);
            in.readObjectStop(errorContainerLabel);
        }
        boolean promise = in.readBoolean(IS_PROMISE, false);
        ArgContainer result = new ArgContainer(dataType, valueType, taskId, isReady, promise, value);
        result.setErrorContainer(errorContainer);
        return result;
    }

    private ArgContainer compositeRead(BDataInput in, ArgContainer argContainer) {
        int arrayLabel = in.readArray(COMPOSITE_VALUE);
        if (arrayLabel == -1) {
            return argContainer;
        }

        int arraySize = in.readArraySize();
        ArgContainer[] args = new ArgContainer[arraySize];

        for (int i = 0; i < arraySize; i++) {
            int readObjLabel = in.readObject(i);
            args[i] = deserializePlain(in);
            in.readObjectStop(readObjLabel);
        }
        in.readArrayStop(arrayLabel);

        argContainer.setCompositeValue(args);

        return argContainer;

    }

}

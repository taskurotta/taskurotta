package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ErrorContainer;

import java.util.UUID;

/**
 * Created by greg on 03/02/15.
 */
public class ArgContainerSerializer implements StreamBSerializer<ArgContainer> {

    private final CString TASK_ID = new CString("tId");
    private final CString DATA_TYPE = new CString("dTy");
    private final CString COMPOSITE_VALUE = new CString("coVal");
    private final CString JSON_VALUE = new CString("jVal");
    private final CString ERROR_CONTAINER = new CString("erCo");
    private final CString VALUE_TYPE = new CString("vTy");

    private ErrorContainerSerializer errorContainerSerializer = new ErrorContainerSerializer();

    @Override
    public Class<ArgContainer> getObjectClass() {
        return ArgContainer.class;
    }

    @Override
    public void write(BDataOutput out, ArgContainer object) {
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeString(DATA_TYPE, object.getDataType());
        writeArgContainersArray(COMPOSITE_VALUE, out, object.getCompositeValue());
        out.writeString(JSON_VALUE, object.getJSONValue());
        if (object.getErrorContainer() != null) {
            int errorContainerLabel = out.writeObject(ERROR_CONTAINER);
            errorContainerSerializer.write(out, object.getErrorContainer());
            out.writeObjectStop(errorContainerLabel);
        }
        if (object.getValueType() != null) {
            int valueLabel = out.writeObject(VALUE_TYPE);
            out.writeInt(VALUE_TYPE, object.getValueType().getValue());
            out.writeObjectStop(valueLabel);
        }
    }

    @Override
    public ArgContainer read(BDataInput in) {
        UUID taskId = in.readUUID(TASK_ID);
        String dataType = in.readString(DATA_TYPE);
        ArgContainer[] args = readArgContainersArray(COMPOSITE_VALUE, in);
        String jsonValue = in.readString(JSON_VALUE);
        int errorContainerLabel = in.readObject(ERROR_CONTAINER);
        ErrorContainer errorContainer = null;
        if (errorContainerLabel != -1) {
            errorContainer = errorContainerSerializer.read(in);
            in.readObjectStop(errorContainerLabel);
        }
        int valueType = -1;
        int valueLabel = in.readObject(VALUE_TYPE);
        if (valueLabel > 0) {
            valueType = in.readInt(VALUE_TYPE);
            in.readObjectStop(valueLabel);
        }
        ArgContainer argContainer = new ArgContainer();
        argContainer.setTaskId(taskId);
        argContainer.setDataType(dataType);
        argContainer.setCompositeValue(args);
        argContainer.setJSONValue(jsonValue);
        argContainer.setErrorContainer(errorContainer);
        if (valueType != -1) {
            argContainer.setValueType(ArgContainer.ValueType.fromInt(valueType));
        }
        return argContainer;
    }


    public ArgContainer[] readArgContainersArray(CString name, BDataInput in) {
        int arrayLabel = in.readArray(name);
        if (arrayLabel == -1) {
            return null;
        }
        int arraySize = in.readArraySize();
        ArgContainer[] args = null;
        if (arrayLabel > 0) {
            args = new ArgContainer[arraySize];
            for (int i = 0; i < arraySize; i++) {
                int readObjLabel = in.readObject(SerializerTools.createCString(i));
                ArgContainer argCont = read(in);
                args[i] = argCont;
                in.readObjectStop(readObjLabel);
            }
        }
        in.readArrayStop(arrayLabel);
        return args;
    }

    public void writeArgContainersArray(CString arrayName, BDataOutput out, ArgContainer[] argContainers) {
        if (argContainers != null) {
            int arrayLabel = out.writeArray(arrayName);
            for (int i = 0; i < argContainers.length; i++) {
                int objectStart = out.writeObject(SerializerTools.createCString(i)); //todo optimize that
                ArgContainer argContainer = argContainers[i];
                write(out, argContainer);
                out.writeObjectStop(objectStart);
            }
            out.writeArrayStop(arrayLabel);
        }
    }

}

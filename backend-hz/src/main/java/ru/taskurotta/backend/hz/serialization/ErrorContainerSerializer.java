package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.ErrorContainer;

import java.io.IOException;

import static ru.taskurotta.backend.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.backend.hz.serialization.SerializationTools.writeString;

/**
 * User: greg
 */
public class ErrorContainerSerializer implements StreamSerializer<ErrorContainer> {

    @Override
    public void write(ObjectDataOutput out, ErrorContainer object) throws IOException {
        out.writeInt(object.getClassNames().length);
        for (String name : object.getClassNames()) {
            writeString(out, name);
        }
        writeString(out, object.getMessage());
        writeString(out, object.getStackTrace());
    }

    @Override
    public ErrorContainer read(ObjectDataInput in) throws IOException {
        ErrorContainer errorContainer = new ErrorContainer();
        int length = in.readInt();
        String[] types = new String[length];
        for (int i=0; i< length; i++) {
            types[i] = readString(in);
        }
        errorContainer.setClassNames(types);
        errorContainer.setMessage(readString(in));
        errorContainer.setStackTrace(readString(in));
        return errorContainer;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.ERROR_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

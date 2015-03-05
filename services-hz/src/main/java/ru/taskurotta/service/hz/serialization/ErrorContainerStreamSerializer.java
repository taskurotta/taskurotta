package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.ErrorContainer;

import java.io.IOException;

import static ru.taskurotta.service.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeString;

/**
 * User: greg
 */
public class ErrorContainerStreamSerializer implements StreamSerializer<ErrorContainer> {

    @Override
    public void write(ObjectDataOutput out, ErrorContainer object) throws IOException {

        if (object == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);

        out.writeInt(object.getClassNames().length);
        for (String name : object.getClassNames()) {
            writeString(out, name);
        }
        writeString(out, object.getMessage());
        writeString(out, object.getStackTrace());
        out.writeBoolean(object.isFatalError());
    }

    @Override
    public ErrorContainer read(ObjectDataInput in) throws IOException {

        if (!in.readBoolean()) {
            return null;
        }

        int length = in.readInt();
        String[] types = new String[length];
        for (int i=0; i< length; i++) {
            types[i] = readString(in);
        }
        String message = readString(in);
        String stackTrace = readString(in);
        boolean fatalError = in.readBoolean();

        return new ErrorContainer(types, message, stackTrace, fatalError);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.ERROR_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

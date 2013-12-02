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
public class ErrorContainerStreamSerializer implements StreamSerializer<ErrorContainer> {

    @Override
    public void write(ObjectDataOutput out, ErrorContainer object) throws IOException {

        if (object == null) {
            out.writeBoolean(false);
            return;
        }

        out.writeBoolean(true);

        writeString(out, object.getClassName());
        writeString(out, object.getMessage());
        writeString(out, object.getStackTrace());
    }

    @Override
    public ErrorContainer read(ObjectDataInput in) throws IOException {

        if (!in.readBoolean()) {
            return null;
        }

        ErrorContainer errorContainer = new ErrorContainer();
        errorContainer.setClassName(readString(in));
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

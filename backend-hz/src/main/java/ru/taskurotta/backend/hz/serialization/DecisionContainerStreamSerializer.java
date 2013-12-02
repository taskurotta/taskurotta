package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.IOException;
import java.util.UUID;

import static ru.taskurotta.backend.hz.serialization.SerializationTools.*;

/**
 * User: greg
 */
public class DecisionContainerStreamSerializer implements StreamSerializer<DecisionContainer> {

    private final static Logger logger = LoggerFactory.getLogger(DecisionContainerStreamSerializer.class);

    private ArgContainerStreamSerializer argContainerStreamSerializer = new ArgContainerStreamSerializer();
    private ErrorContainerStreamSerializer errorContainerSerializer = new ErrorContainerStreamSerializer();

    @Override
    public void write(ObjectDataOutput out, DecisionContainer object) throws IOException {

        try {
            UUIDSerializer.write(out, object.getTaskId());
            UUIDSerializer.write(out, object.getProcessId());
            argContainerStreamSerializer.write(out, object.getValue());
            errorContainerSerializer.write(out, object.getErrorContainer());
            out.writeLong(object.getExecutionTime());
            out.writeLong(object.getRestartTime());
            writeString(out, object.getActorId());
            writeTaskContainerArray(out, object.getTasks());
        } catch (RuntimeException e) {
            logger.error("Cannot write data", e);
            throw e;
        } catch (IOException e) {
            logger.error("Cannot write data", e);
            throw e;
        }

    }

    @Override
    public DecisionContainer read(ObjectDataInput in) throws IOException {

        try {
            UUID taskId = UUIDSerializer.read(in);
            UUID processId = UUIDSerializer.read(in);
            ArgContainer value = argContainerStreamSerializer.read(in);
            ErrorContainer errorContainer = errorContainerSerializer.read(in);
            long exTime = in.readLong();
            long reTime = in.readLong();
            String actorId = readString(in);
            TaskContainer[] taskContainers = readTaskContainerArray(in);
            return new DecisionContainer(taskId, processId, value, errorContainer, reTime, taskContainers, actorId, exTime);
        } catch (RuntimeException e) {
            logger.error("Cannot read data", e);
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            logger.error("Cannot read data", e);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.DECISION_CONTAINER;
    }

    @Override
    public void destroy() {

    }
}

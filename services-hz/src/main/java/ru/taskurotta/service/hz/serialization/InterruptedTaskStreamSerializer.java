package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.console.model.InterruptedTask;

import java.io.IOException;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 10:49 PM
 */
public class InterruptedTaskStreamSerializer implements StreamSerializer<InterruptedTask> {

    @Override
    public void write(ObjectDataOutput out, InterruptedTask itdTask) throws IOException {

        UUIDSerializer.write(out, itdTask.getTaskId());
        UUIDSerializer.write(out, itdTask.getProcessId());
        SerializationTools.writeString(out, itdTask.getActorId());
        SerializationTools.writeString(out, itdTask.getStarterId());
        out.writeLong(itdTask.getTime());
        SerializationTools.writeString(out, itdTask.getErrorMessage());
        SerializationTools.writeString(out, itdTask.getErrorClassName());
        SerializationTools.writeString(out, itdTask.getStackTrace());

    }

    @Override
    public InterruptedTask read(ObjectDataInput in) throws IOException {

        InterruptedTask result = new InterruptedTask();

        result.setTaskId(UUIDSerializer.read(in));
        result.setProcessId(UUIDSerializer.read(in));
        result.setActorId(SerializationTools.readString(in));
        result.setStarterId(SerializationTools.readString(in));
        result.setTime(in.readLong());
        result.setErrorMessage(SerializationTools.readString(in));
        result.setErrorClassName(SerializationTools.readString(in));
        result.setStackTrace(SerializationTools.readString(in));

        return result;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.BROKEN_PROCESS;
    }

    @Override
    public void destroy() {
    }
}

package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.console.model.InterruptedTaskExt;

import java.io.IOException;

/**
 * Created on 25.05.2015.
 */
public class InterruptedTaskExtStreamSerializer implements StreamSerializer<InterruptedTaskExt> {

    @Override
    public void write(ObjectDataOutput out, InterruptedTaskExt itdTask) throws IOException {
        UUIDSerializer.write(out, itdTask.getTaskId());
        UUIDSerializer.write(out, itdTask.getProcessId());
        SerializationTools.writeString(out, itdTask.getActorId());
        SerializationTools.writeString(out, itdTask.getStarterId());
        out.writeLong(itdTask.getTime());
        SerializationTools.writeString(out, itdTask.getErrorMessage());
        SerializationTools.writeString(out, itdTask.getErrorClassName());

        SerializationTools.writeString(out, itdTask.getFullMessage());
        SerializationTools.writeString(out, itdTask.getStackTrace());
    }

    @Override
    public InterruptedTaskExt read(ObjectDataInput in) throws IOException {
        InterruptedTaskExt result = new InterruptedTaskExt();

        result.setTaskId(UUIDSerializer.read(in));
        result.setProcessId(UUIDSerializer.read(in));
        result.setActorId(SerializationTools.readString(in));
        result.setStarterId(SerializationTools.readString(in));
        result.setTime(in.readLong());
        result.setErrorMessage(SerializationTools.readString(in));
        result.setErrorClassName(SerializationTools.readString(in));

        result.setFullMessage(SerializationTools.readString(in));
        result.setStackTrace(SerializationTools.readString(in));

        return result;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.BROKEN_TASK_EXT;
    }

    @Override
    public void destroy() {

    }
}

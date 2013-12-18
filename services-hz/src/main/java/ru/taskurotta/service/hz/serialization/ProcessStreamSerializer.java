package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.console.model.Process;

import java.io.IOException;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 10:49 PM
 */
public class ProcessStreamSerializer implements StreamSerializer<Process> {

    private TaskContainerStreamSerializer taskContainerStreamSerializer = new TaskContainerStreamSerializer();

    @Override
    public void write(ObjectDataOutput out, Process process) throws IOException {

        UUIDSerializer.write(out, process.getProcessId());
        UUIDSerializer.write(out, process.getStartTaskId());
        SerializationTools.writeString(out, process.getCustomId());
        out.writeLong(process.getStartTime());
        out.writeLong(process.getEndTime());
        out.writeInt(process.getState());
        SerializationTools.writeString(out, process.getReturnValue());
        taskContainerStreamSerializer.write(out, process.getStartTask());

    }

    @Override
    public Process read(ObjectDataInput in) throws IOException {

        Process process = new Process();
        process.setProcessId(UUIDSerializer.read(in));
        process.setStartTaskId(UUIDSerializer.read(in));
        process.setCustomId(SerializationTools.readString(in));
        process.setStartTime(in.readLong());
        process.setEndTime(in.readLong());
        process.setState(in.readInt());
        process.setReturnValue(SerializationTools.readString(in));
        process.setStartTask(taskContainerStreamSerializer.read(in));

        return process;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.PROCESS;
    }

    @Override
    public void destroy() {
    }
}

package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.console.model.BrokenProcess;

import java.io.IOException;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 10:49 PM
 */
public class BrokenProcessStreamSerializer implements StreamSerializer<BrokenProcess> {

    @Override
    public void write(ObjectDataOutput out, BrokenProcess brokenProcess) throws IOException {

        UUIDSerializer.write(out, brokenProcess.getProcessId());
        SerializationTools.writeString(out, brokenProcess.getStartActorId());
        SerializationTools.writeString(out, brokenProcess.getBrokenActorId());
        out.writeLong(brokenProcess.getTime());
        SerializationTools.writeString(out, brokenProcess.getErrorMessage());
        SerializationTools.writeString(out, brokenProcess.getErrorClassName());
        SerializationTools.writeString(out, brokenProcess.getStackTrace());

    }

    @Override
    public BrokenProcess read(ObjectDataInput in) throws IOException {

        BrokenProcess brokenProcess = new BrokenProcess();

        brokenProcess.setProcessId(UUIDSerializer.read(in));
        brokenProcess.setStartActorId(SerializationTools.readString(in));
        brokenProcess.setBrokenActorId(SerializationTools.readString(in));
        brokenProcess.setTime(in.readLong());
        brokenProcess.setErrorMessage(SerializationTools.readString(in));
        brokenProcess.setErrorClassName(SerializationTools.readString(in));
        brokenProcess.setStackTrace(SerializationTools.readString(in));

        return brokenProcess;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.BROKEN_PROCESS;
    }

    @Override
    public void destroy() {
    }
}

package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.transport.model.Decision;

import java.io.IOException;
import java.util.UUID;

/**
 */
public class DecisionStreamSerializer implements StreamSerializer<Decision> {

    private DecisionContainerStreamSerializer decisionContainerStreamSerializer = new
            DecisionContainerStreamSerializer();

    @Override
    public void write(ObjectDataOutput out, Decision object) throws IOException {
        UUIDSerializer.write(out, object.getTaskId());
        UUIDSerializer.write(out, object.getProcessId());
        out.writeByte(object.getState());

        UUID pass = object.getPass();
        if (pass == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            UUIDSerializer.write(out, pass);
        }

        out.writeLong(object.getRecoveryTime());
        out.writeInt(object.getErrorAttempts());

        decisionContainerStreamSerializer.write(out, object.getDecisionContainer());
    }

    @Override
    public Decision read(ObjectDataInput in) throws IOException {

        UUID taskId = UUIDSerializer.read(in);
        UUID processId = UUIDSerializer.read(in);
        int state = in.readByte();
        UUID pass = null;

        boolean hasPass = in.readBoolean();
        if (hasPass) {
            pass = UUIDSerializer.read(in);
        }

        long recoveryTime = in.readLong();
        int errorAttempts = in.readInt();

        return new Decision(taskId, processId, state, pass, recoveryTime, errorAttempts,
                decisionContainerStreamSerializer.read(in));
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.DECISION;
    }

    @Override
    public void destroy() {

    }
}

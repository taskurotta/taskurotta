package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.Decision;

import java.util.UUID;

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readObject;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeObjectIfNotNull;

/**
 */
public class DecisionBSerializer implements StreamBSerializer<Decision> {

    public static final CString TASK_ID = new CString("t");
    public static final CString PROCESS_ID = new CString("p");
    public static final CString STATE = new CString("state");
    public static final CString PASS = new CString("pass");
    public static final CString RECOVERY_TIME = new CString("rTime");
    public static final CString DECISION_CONTAINER = new CString("c");

    private DecisionContainerBSerializer decisionContainerBSerializer = new DecisionContainerBSerializer();

    @Override
    public Class<Decision> getObjectClass() {
        return Decision.class;
    }

    @Override
    public void write(BDataOutput out, Decision object) {

        int writeIdLabel = out.writeObject(_ID);
        out.writeUUID(TASK_ID, object.getTaskId());
        out.writeUUID(PROCESS_ID, object.getProcessId());
        out.writeObjectStop(writeIdLabel);

        out.writeInt(STATE, object.getState());
        out.writeUUID(PASS, object.getPass());

        out.writeLong(RECOVERY_TIME, object.getRecoveryTime(), 0);

        writeObjectIfNotNull(DECISION_CONTAINER, object.getDecisionContainer(), decisionContainerBSerializer, out);
    }

    @Override
    public Decision read(BDataInput in) {

        int readIdLabel = in.readObject(_ID);
        UUID taskId = in.readUUID(TASK_ID);
        UUID processId = in.readUUID(PROCESS_ID);
        in.readObjectStop(readIdLabel);

        int state = in.readInt(STATE);
        UUID pass = in.readUUID(PASS);

        long recoveryTime = in.readLong(RECOVERY_TIME);

        return new Decision(taskId, processId, state, pass, recoveryTime, readObject(DECISION_CONTAINER, decisionContainerBSerializer, in));
    }
}

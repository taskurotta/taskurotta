package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.hz.server.HazelcastTaskServer.ProcessDecisionUnitOfWork;

import java.io.IOException;

/**
 * User: stukushin
 * Date: 23.01.2015
 * Time: 14:16
 */

public class ProcessDecisionUnitOfWorkStreamSerializer implements StreamSerializer<ProcessDecisionUnitOfWork> {

    private TaskKeyStreamSerializer taskKeyStreamSerializer = new TaskKeyStreamSerializer();

    @Override
    public void write(ObjectDataOutput out, ProcessDecisionUnitOfWork object) throws IOException {
        taskKeyStreamSerializer.write(out, object.getTaskKey());
    }

    @Override
    public ProcessDecisionUnitOfWork read(ObjectDataInput in) throws IOException {
        return new ProcessDecisionUnitOfWork(taskKeyStreamSerializer.read(in));
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.PROCESS_DECISION_UNIT_OF_WORK;
    }

    @Override
    public void destroy() {}
}

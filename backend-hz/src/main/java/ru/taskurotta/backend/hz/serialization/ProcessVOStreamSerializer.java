package ru.taskurotta.backend.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.backend.console.model.ProcessVO;

import java.io.IOException;

/**
 * User: romario
 * Date: 12/1/13
 * Time: 10:49 PM
 */
public class ProcessVOStreamSerializer implements StreamSerializer<ProcessVO> {

    private TaskContainerStreamSerializer taskContainerStreamSerializer = new TaskContainerStreamSerializer();

    @Override
    public void write(ObjectDataOutput out, ProcessVO processVO) throws IOException {

        SerializationTools.writeString(out, processVO.getCustomId());
        out.writeLong(processVO.getEndTime());
        UUIDSerializer.write(out, processVO.getProcessUuid());
        SerializationTools.writeString(out, processVO.getReturnValueJson());
        taskContainerStreamSerializer.write(out, processVO.getStartTask());
        UUIDSerializer.write(out, processVO.getStartTaskUuid());
        out.writeLong(processVO.getStartTime());

    }

    @Override
    public ProcessVO read(ObjectDataInput in) throws IOException {

        ProcessVO processVO = new ProcessVO();
        processVO.setCustomId(SerializationTools.readString(in));
        processVO.setEndTime(in.readLong());
        processVO.setProcessUuid(UUIDSerializer.read(in));
        processVO.setReturnValueJson(SerializationTools.readString(in));
        processVO.setStartTask(taskContainerStreamSerializer.read(in));
        processVO.setStartTaskUuid(UUIDSerializer.read(in));
        processVO.setStartTime(in.readLong());

        return processVO;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.PROCESS_VO;
    }

    @Override
    public void destroy() {
    }
}

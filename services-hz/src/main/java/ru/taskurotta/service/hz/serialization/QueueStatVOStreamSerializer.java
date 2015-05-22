package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.console.model.QueueStatVO;

import java.io.IOException;

import static ru.taskurotta.service.hz.serialization.SerializationTools.readDate;
import static ru.taskurotta.service.hz.serialization.SerializationTools.readString;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeDate;
import static ru.taskurotta.service.hz.serialization.SerializationTools.writeString;

/**
 * User: stukushin
 * Date: 22.05.2015
 * Time: 15:42
 */

public class QueueStatVOStreamSerializer implements StreamSerializer<QueueStatVO> {
    @Override
    public void write(ObjectDataOutput out, QueueStatVO object) throws IOException {
        writeString(out, object.getName());
        out.writeInt(object.getCount());
        writeDate(out, object.getLastActivity());
        out.writeLong(object.getLastPolledTaskEnqueueTime());
        out.writeLong(object.getInHour());
        out.writeLong(object.getOutHour());
        out.writeLong(object.getInDay());
        out.writeLong(object.getOutDay());
        out.writeInt(object.getNodes());
        out.writeBoolean(object.isLocal());
    }

    @Override
    public QueueStatVO read(ObjectDataInput in) throws IOException {
        QueueStatVO queueStatVO = new QueueStatVO();
        queueStatVO.setName(readString(in));
        queueStatVO.setCount(in.readInt());
        queueStatVO.setLastActivity(readDate(in));
        queueStatVO.setLastPolledTaskEnqueueTime(in.readLong());
        queueStatVO.setInHour(in.readLong());
        queueStatVO.setOutHour(in.readLong());
        queueStatVO.setInDay(in.readLong());
        queueStatVO.setOutDay(in.readLong());
        queueStatVO.setNodes(in.readInt());
        queueStatVO.setLocal(in.readBoolean());
        return queueStatVO;
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.QUEUE_STAT_VO;
    }

    @Override
    public void destroy() {

    }
}

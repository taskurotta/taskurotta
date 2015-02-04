package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.queue.TaskQueueItem;

import java.io.IOException;
import java.util.Date;

public class TaskQueueItemSteamSerializer implements StreamSerializer<TaskQueueItem> {


    @Override
    public TaskQueueItem read(ObjectDataInput in) throws IOException {
        TaskQueueItem item = new TaskQueueItem();
        item.setTaskId(UUIDSerializer.read(in));
        item.setProcessId(UUIDSerializer.read(in));
        item.setEnqueueTime(in.readLong());
        item.setStartTime(in.readLong());
        item.setQueueName(in.readUTF());
        long millis = in.readLong();
        item.setCreatedDate((millis > 0) ? new Date(millis) : null);
        item.setTaskList(in.readUTF());
        return item;
    }

    @Override
    public void write(ObjectDataOutput out, TaskQueueItem taskQueueItem) throws IOException {
        UUIDSerializer.write(out, taskQueueItem.getTaskId());
        UUIDSerializer.write(out, taskQueueItem.getProcessId());
        out.writeLong(taskQueueItem.getEnqueueTime());
        out.writeLong(taskQueueItem.getStartTime());
        out.writeUTF(taskQueueItem.getQueueName());
        out.writeLong((taskQueueItem.getCreatedDate() != null) ? taskQueueItem.getCreatedDate().getTime() : -1);
        out.writeUTF(taskQueueItem.getTaskList());
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.TASK_QUEUE_ITEM;
    }

    @Override
    public void destroy() {
    }
}

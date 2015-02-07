package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.queue.TaskQueueItem;

/**
 */
public class TaskQueueItemStreamBSerializer implements StreamBSerializer<TaskQueueItem> {

    public static final CString TASK_ID = new CString("t");
    public static final CString PROCESS_ID = new CString("p");
    public static final CString ENQUEUE_TIME = new CString("et");
    public static final CString START_TIME = new CString("st");
    public static final CString QUEUE_NAME = new CString("q");
    public static final CString CREATE_DATE = new CString("c");
    public static final CString TASK_LIST = new CString("l");

    @Override
    public Class<TaskQueueItem> getObjectClass() {
        return TaskQueueItem.class;
    }

    @Override
    public void write(BDataOutput out, TaskQueueItem taskQueueItem) {

        out.writeUUID(TASK_ID, taskQueueItem.getTaskId());
        out.writeUUID(PROCESS_ID, taskQueueItem.getProcessId());
        out.writeLong(ENQUEUE_TIME, taskQueueItem.getEnqueueTime());

        final long startTime = taskQueueItem.getStartTime();
        if (startTime != 0) {
            out.writeLong(START_TIME, startTime);
        }

        out.writeString(QUEUE_NAME, taskQueueItem.getQueueName());
        out.writeDate(CREATE_DATE, taskQueueItem.getCreatedDate());
        out.writeString(TASK_LIST, taskQueueItem.getTaskList());

    }

    @Override
    public TaskQueueItem read(BDataInput in) {

        TaskQueueItem taskQueueItem = new TaskQueueItem();

        taskQueueItem.setTaskId(in.readUUID(TASK_ID));
        taskQueueItem.setProcessId(in.readUUID(PROCESS_ID));
        taskQueueItem.setEnqueueTime(in.readLong(ENQUEUE_TIME));
        taskQueueItem.setStartTime(in.readLong(START_TIME));
        taskQueueItem.setQueueName(in.readString(QUEUE_NAME));
        taskQueueItem.setCreatedDate(in.readDate(CREATE_DATE));
        taskQueueItem.setTaskList(in.readString(TASK_LIST));

        return taskQueueItem;
    }
}
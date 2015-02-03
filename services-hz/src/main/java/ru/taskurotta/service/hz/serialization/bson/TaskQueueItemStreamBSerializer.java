package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.service.queue.TaskQueueItem;

import java.util.Date;

/**
 */
public class TaskQueueItemStreamBSerializer implements StreamBSerializer<TaskQueueItem> {

    public static final CString TASK_ID = new CString("tId");
    public static final CString PROCESS_ID = new CString("pId");
    public static final CString ENQUEUE_TIME = new CString("eTime");
    public static final CString START_TIME = new CString("sTime");
    public static final CString QUEUE_NAME = new CString("qName");
    public static final CString CREATE_DATE = new CString("cDate");
    public static final CString TASK_LIST = new CString("tList");

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

        final String queueName = taskQueueItem.getQueueName();
        if (queueName != null) {
            out.writeString(QUEUE_NAME, queueName);
        }

        final Date createdDate = taskQueueItem.getCreatedDate();
        if (createdDate != null) {
            out.writeDate(CREATE_DATE, createdDate);
        }

        final String taskList = taskQueueItem.getTaskList();
        if (taskList != null) {
            out.writeString(TASK_LIST, taskList);
        }

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
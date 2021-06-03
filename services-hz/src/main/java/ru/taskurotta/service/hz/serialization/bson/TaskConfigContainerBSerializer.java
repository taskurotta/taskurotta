package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;

import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.readObject;
import static ru.taskurotta.service.hz.serialization.bson.BSerializerTools.writeObjectIfNotNull;

public class TaskConfigContainerBSerializer implements StreamBSerializer<TaskConfigContainer> {

    private static final CString CUSTOM_ID = new CString("customId");
    private static final CString START_TIME = new CString("startTime");
    private static final CString TASK_LIST = new CString("taskList");
    private static final CString RETRY_POLICY = new CString("retryPolicy");
    private static final CString TIMEOUT = new CString("timeout");
    private static final CString IDEMPOTENCY_KEY = new CString("idempotencyKey");

    RetryPolicyConfigContainerBSerializer retryPolicyConfigContainerBSerializer = new RetryPolicyConfigContainerBSerializer();


    @Override
    public Class<TaskConfigContainer> getObjectClass() {
        return TaskConfigContainer.class;
    }

    @Override
    public void write(BDataOutput out, TaskConfigContainer object) {
        out.writeString(CUSTOM_ID, object.getCustomId());
        out.writeLong(START_TIME, object.getStartTime(), -1L);
        out.writeString(TASK_LIST, object.getTaskList());
        out.writeString(IDEMPOTENCY_KEY, object.getIdempotenceKey());
        writeObjectIfNotNull(RETRY_POLICY, object.getRetryPolicyConfigContainer(),
                retryPolicyConfigContainerBSerializer, out);
        out.writeLong(TIMEOUT, object.getTimeout(), -1L);
    }

    @Override
    public TaskConfigContainer read(BDataInput in) {
        String customId = in.readString(CUSTOM_ID);
        long startTime = in.readLong(START_TIME, -1L);
        String taskList = in.readString(TASK_LIST);
        String idempotencyKey = in.readString(IDEMPOTENCY_KEY);
        RetryPolicyConfigContainer retryPolicyConfigContainer = readObject(RETRY_POLICY,
                retryPolicyConfigContainerBSerializer, in);
        long timeout = in.readLong(TIMEOUT, -1L);

        return new TaskConfigContainer(customId, startTime, taskList, idempotencyKey, retryPolicyConfigContainer, timeout);
    }
}

package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.transport.model.RetryPolicyConfigContainer;
import ru.taskurotta.transport.model.TaskConfigContainer;

/**
 * Created by greg on 03/02/15.
 */
public class TaskConfigContainerSerializer implements StreamBSerializer<TaskConfigContainer> {

    private static final CString CUSTOM_ID = new CString("customId");
    private static final CString START_TIME = new CString("startTime");
    private static final CString TASK_LIST = new CString("taskList");
    private static final CString RETRY_POLICY_CONFIG_CONTAINER = new CString("retryPolicyConfigContainer");

    RetryPolicyConfigContainerSerializer retryPolicyConfigContainerSerializer = new RetryPolicyConfigContainerSerializer();

    @Override
    public void write(BDataOutput out, TaskConfigContainer object) {
        out.writeString(CUSTOM_ID, object.getCustomId());
        out.writeLong(START_TIME, object.getStartTime());
        out.writeString(TASK_LIST, object.getTaskList());
        int retryPolicyConfigContainerObject = out.writeObject(RETRY_POLICY_CONFIG_CONTAINER);
        retryPolicyConfigContainerSerializer.write(out, object.getRetryPolicyConfigContainer());
        out.writeObjectStop(retryPolicyConfigContainerObject);

    }

    @Override
    public TaskConfigContainer read(BDataInput in) {
        String customId = in.readString(CUSTOM_ID);
        long startTime = in.readLong(START_TIME);
        String taskList = in.readString(TASK_LIST);
        int retryContainerLabel = in.readObject(RETRY_POLICY_CONFIG_CONTAINER);
        RetryPolicyConfigContainer configContainer = retryPolicyConfigContainerSerializer.read(in);
        in.readObjectStop(retryContainerLabel);
        TaskConfigContainer taskConfigContainer = new TaskConfigContainer();
        taskConfigContainer.setCustomId(customId);
        taskConfigContainer.setStartTime(startTime);
        taskConfigContainer.setTaskList(taskList);
        taskConfigContainer.setRetryPolicyConfigContainer(configContainer);
        return taskConfigContainer;
    }
}

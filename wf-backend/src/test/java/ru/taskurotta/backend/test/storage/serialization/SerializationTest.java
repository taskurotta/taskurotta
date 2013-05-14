package ru.taskurotta.backend.test.storage.serialization;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.backend.storage.model.serialization.JsonSerializer;
import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.TaskType;

/**
 * User: moroz
 * Date: 11.04.13
 */
public class SerializationTest {

    public static TaskContainer createTaskContainer() {
        UUID originalUuid = UUID.randomUUID();
        UUID processUuid = UUID.randomUUID();
        TaskType originalTaskType = TaskType.WORKER;
        String originalName = "test.me.worker";
        String originalVersion = "7.6.5";
        String originalMethod = "doSomeWork";
        String originalActorId = originalName + "#" + originalVersion;
        long originalStartTime = System.currentTimeMillis();
        int originalNumberOfAttempts = 5;

        String origArg1ClassName = "null";
        String origArg1Value = "null";
        ArgContainer originalArg1 = new ArgContainer(origArg1ClassName, ArgContainer.ValueType.PROMISE, originalUuid, false, origArg1Value);

        String origArg2ClassName = "java.lang.String";
        String origArg2Value = "string value here";
        ArgContainer originalArg2 = new ArgContainer(origArg2ClassName, ArgContainer.ValueType.PLAIN, originalUuid, true, origArg2Value);


        ArgType[] argTypes = new ArgType[]{ArgType.WAIT, ArgType.NONE};
        TaskOptionsContainer originalOptions = new TaskOptionsContainer(argTypes);

        return new TaskContainer(originalUuid, processUuid, originalMethod, originalActorId, originalTaskType, originalStartTime, originalNumberOfAttempts, new ArgContainer[]{originalArg1, originalArg2}, originalOptions);
    }

    @Test
    public void testSerialization() {
        TaskContainer taskContainer = createTaskContainer();
        JsonSerializer<TaskContainer> serializer = new JsonSerializer<TaskContainer>(TaskContainer.class);
        String json = (String) serializer.serialize(taskContainer);
        System.out.println(json);
        Assert.assertNotNull(json);
        TaskContainer dTaskContainer = serializer.deserialize(json);
        Assert.assertEquals(dTaskContainer.getTaskId(), taskContainer.getTaskId());
        Assert.assertEquals(dTaskContainer.getProcessId(), taskContainer.getProcessId());
    }


}

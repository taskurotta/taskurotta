package ru.taskurotta.service.test.storage.serialization;

import org.junit.Assert;
import org.junit.Test;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.internal.core.ArgType;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.transport.model.serialization.JsonSerializer;

import java.util.UUID;

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
        ArgContainer originalArg1 = new ArgContainer(origArg1ClassName, ArgContainer.ValueType.PLAIN, originalUuid, false, true, origArg1Value);

        String origArg2ClassName = "java.lang.String";
        String origArg2Value = "string value here";
        ArgContainer originalArg2 = new ArgContainer(origArg2ClassName, ArgContainer.ValueType.PLAIN, originalUuid, true, false, origArg2Value);


        ArgType[] argTypes = new ArgType[]{ArgType.WAIT, ArgType.NONE};
        TaskOptionsContainer originalOptions = new TaskOptionsContainer(argTypes);
        String[] failTypes = {"java.lang.RuntimeException"};

        return new TaskContainer(originalUuid, processUuid, originalMethod, originalActorId, originalTaskType, originalStartTime, originalNumberOfAttempts, new ArgContainer[]{originalArg1, originalArg2}, originalOptions, true, failTypes);
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

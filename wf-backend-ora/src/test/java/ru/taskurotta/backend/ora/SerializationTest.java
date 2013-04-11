package ru.taskurotta.backend.ora;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;

/**
 * User: moroz
 * Date: 09.04.13
 */
public class SerializationTest {

    static TaskContainer createTaskContainer() {
        UUID originalUuid = UUID.randomUUID();
        TaskType originalTaskType = TaskType.WORKER;
        String originalName = "test.me.worker";
        String originalVersion = "7.6.5";
        String originalMethod = "doSomeWork";
        TaskTarget originalTaskTarget = new TaskTargetImpl(originalTaskType, originalName, originalVersion, originalMethod);
        long originalStartTime = System.currentTimeMillis();
        int originalNumberOfAttempts = 5;

        String origArg1ClassName = "null";
        String origArg1Value = "null";
        ArgContainer originalArg1 = new ArgContainer(origArg1ClassName, true, originalUuid, false, origArg1Value);

        String origArg2ClassName = "java.lang.String";
        String origArg2Value = "string value here";
        ArgContainer originalArg2 = new ArgContainer(origArg2ClassName, false, originalUuid, true, origArg2Value);


        ArgType[] argTypes = new ArgType[]{ArgType.WAIT, ArgType.NONE};
        TaskOptionsContainer originalOptions = new TaskOptionsContainer(argTypes);

        return new TaskContainer(originalUuid, originalTaskTarget, originalStartTime, originalNumberOfAttempts, new ArgContainer[]{originalArg1, originalArg2}, originalOptions);
    }

    @Test
    public void test() {
        ObjectMapper mapper = new ObjectMapper();
        TaskContainer container = createTaskContainer();
        try {
            long startTime = new Date().getTime();
            for (int i = 0; i < 20000; i++) {
                String json = mapper.writeValueAsString(container);
                json.trim();
            }
            long endTime = new Date().getTime();
            System.out.println("--------------------------------------------------------------");
            System.out.println("20 000 time: " + (endTime - startTime));
            System.out.println("Serializations per sec: " + (endTime - startTime) / 20000f);

            startTime = new Date().getTime();
            for (int i = 0; i < 10000; i++) {
                String json = mapper.writeValueAsString(container);
                json.trim();
            }
            endTime = new Date().getTime();
            System.out.println("--------------------------------------------------------------");
            System.out.println("10 000 time: " + (endTime - startTime));
            System.out.println("Serializations per sec: " + (endTime - startTime) / 10000f);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}

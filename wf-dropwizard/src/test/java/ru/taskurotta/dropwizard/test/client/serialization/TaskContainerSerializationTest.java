package ru.taskurotta.dropwizard.test.client.serialization;

import java.io.File;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.dropwizard.client.serialization.wrapper.TaskContainerWrapper;
import ru.taskurotta.internal.core.TaskTargetImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskContainerSerializationTest {

    private static final Logger logger = LoggerFactory.getLogger(TaskContainerSerializationTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testTaskContainerDeserialization() {
        ObjectMapper jacksonMapper = new ObjectMapper();

        TaskContainer original = createTaskContainer();

        TaskContainerWrapper resultWrapper = null;
        try {
            File tmpJsonFile = tmpFolder.newFile();
            jacksonMapper.writeValue(tmpJsonFile, new TaskContainerWrapper(original));
            resultWrapper = jacksonMapper.readValue(tmpJsonFile, TaskContainerWrapper.class);
        } catch (Exception e) {
            logger.error("Exception at (de)serialization of TaskContainerWrapper to tmp File", e);
            Assert.fail("Exception at (de)serialization of TaskContainerWrapper to tmp File");
        }

        Assert.assertNotNull(resultWrapper);

        if(resultWrapper != null) {
            TaskContainer result = resultWrapper.getTaskContainer();
            logger.debug("TaskContainer getted is[{}]", result);
            Assert.assertNotNull(result);

            if(result!=null) {
                Assert.assertEquals("Task UUIDs must be the same", original.getTaskId(), result.getTaskId());
                Assert.assertEquals("Start times must be the same", original.getStartTime(), result.getStartTime());
                Assert.assertEquals("Number of attempts must be the same", original.getNumberOfAttempts(), result.getNumberOfAttempts());

                //validateTaskTarget(original.getTarget(), result.getTarget());
                Assert.assertEquals("Task methods must be the same", original.getMethod(), result.getMethod());
                Assert.assertEquals("Task actorIds must be the same", original.getActorId(), result.getActorId());
                Assert.assertEquals("Task types must be the same", original.getType(), result.getType());
                validateTaskArgs(original.getArgs(), result.getArgs());
                validateTaskOptions(original.getOptions(), result.getOptions());

            }
        }

    }

    private TaskContainer createTaskContainer() {
        UUID originalUuid = UUID.randomUUID();
        UUID processUuid = UUID.randomUUID();
        TaskType originalTaskType = TaskType.WORKER;
        String originalName = "test.me.worker";
        String originalVersion = "7.6.5";
        String originalMethod = "doSomeWork";
        String originalActorId = originalName + "#" + originalVersion;
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

        return new TaskContainer(originalUuid, processUuid, originalMethod, originalActorId, originalTaskType,originalStartTime, originalNumberOfAttempts, new ArgContainer[]{originalArg1, originalArg2}, originalOptions);
    }

    //	private void validateTaskTarget(TaskTarget original, TaskTarget validating) {
    //		if(original != null) {
    //			Assert.assertNotNull(validating);
    //			if(validating != null) {
    //				Assert.assertEquals("Task Target methods must be the same", original.getMethod(), validating.getMethod());
    //				Assert.assertEquals("Task Target names must be the same", original.getName(), validating.getName());
    //				Assert.assertEquals("Task Target types must be the same", original.getType(), validating.getType());
    //				Assert.assertEquals("Task Target versions must be the same", original.getVersion(), validating.getVersion());
    //			}
    //
    //		}
    //	}

    private void validateTaskArgs(ArgContainer[] original, ArgContainer[] validating) {
        if(original!=null) {
            Assert.assertNotNull(validating);
            if(validating != null) {
                Assert.assertEquals("Args array size must be the same", original.length, validating.length);
                if(original.length == validating.length) {
                    for(int i = 0;i<validating.length;i++) {
                        validateArgContainer(original[i], validating[i]);
                    }
                }
            }
        }
    }

    private void validateArgContainer(ArgContainer original, ArgContainer validating) {
        if(original!=null) {
            Assert.assertNotNull(validating);
            if(validating!=null) {
                Assert.assertEquals("Arg class names must be the same", original.getClassName(), validating.getClassName());
                Assert.assertEquals("Arg JSON vakues must be the same", original.getJSONValue(), validating.getJSONValue());
                Assert.assertEquals("Arg task UUID must be the same", original.getTaskId(), validating.getTaskId());
            }
        }
    }

    private void validateTaskOptions(TaskOptionsContainer original, TaskOptionsContainer validating) {
        if(original!=null) {
            Assert.assertNotNull(validating);
            if(validating != null) {
                //logger.debug("ArgTypes arrays: original[{}], validating[{}]", original, validating);
                Assert.assertArrayEquals("ArgTypes arrays must be the same", original.getArgTypes(), validating.getArgTypes());
            }
        }
    }

}

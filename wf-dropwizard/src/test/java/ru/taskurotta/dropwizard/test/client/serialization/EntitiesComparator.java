package ru.taskurotta.dropwizard.test.client.serialization;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.TaskTarget;

public class EntitiesComparator {

    private static final Logger logger = LoggerFactory.getLogger(EntitiesComparator.class);

    public static void compare(ArgContainer original, ArgContainer validating) {
        if (original != null) {
            Assert.assertNotNull(validating);
            Assert.assertEquals("Arg class names must be the same", original.getClassName(), validating.getClassName());
            Assert.assertEquals("Arg JSON values must be the same", original.getJSONValue(), validating.getJSONValue());
            Assert.assertEquals("Arg task UUID must be the same", original.getTaskId(), validating.getTaskId());
        }
    }

    public static void compare(TaskOptionsContainer original, TaskOptionsContainer validating) {
        if (original != null) {
            Assert.assertNotNull(validating);
            logger.debug("ArgTypes arrays: original[{}], validating[{}]", original, validating);
            Assert.assertArrayEquals("ArgTypes arrays must be the same", original.getArgTypes(), validating.getArgTypes());
        }
    }

    public static void compare(TaskTarget original, TaskTarget validating) {
        if (original != null) {
            Assert.assertNotNull(validating);
            Assert.assertEquals("Task Target methods must be the same", original.getMethod(), validating.getMethod());
            Assert.assertEquals("Task Target names must be the same", original.getName(), validating.getName());
            Assert.assertEquals("Task Target types must be the same", original.getType(), validating.getType());
            Assert.assertEquals("Task Target versions must be the same", original.getVersion(), validating.getVersion());
        }
    }

    public static void compare(TaskContainer expected, TaskContainer actual) {
        if (expected != null) {
            Assert.assertNotNull(actual);
            Assert.assertEquals("Tasks UUIDs must be the same", expected.getTaskId(), actual.getTaskId());
            Assert.assertEquals("Tasks process UUIDs must be the same", expected.getProcessId(), actual.getProcessId());
            Assert.assertEquals("Start times must be the same", expected.getStartTime(), actual.getStartTime());
            Assert.assertEquals("Number of attempts must be the same", expected.getNumberOfAttempts(), actual.getNumberOfAttempts());

            //validateTaskTarget(original.getTarget(), result.getTarget());
            Assert.assertEquals("Task methods must be the same", expected.getMethod(), actual.getMethod());
            Assert.assertEquals("Task actorIds must be the same", expected.getActorId(), actual.getActorId());
            Assert.assertEquals("Task types must be the same", expected.getType(), actual.getType());
            compare(expected.getArgs(), actual.getArgs());
            compare(expected.getOptions(), actual.getOptions());

        } else {
            Assert.assertNull(actual);
        }
    }

    public static void compare(ArgContainer[] expected, ArgContainer[] actual) {
        if (expected != null) {
            Assert.assertNotNull(actual);
            Assert.assertEquals("Args array size must be the same", expected.length, actual.length);
            if (expected.length == actual.length) {
                for (int i = 0; i < actual.length; i++) {
                    compare(expected[i], actual[i]);
                }
            }
        }
    }

    public static void compare(DecisionContainer expected, DecisionContainer actual) {
        if (expected != null) {
            Assert.assertNotNull(actual);
            Assert.assertEquals("DecisionContainer taskId must be equal", expected.getTaskId(), actual.getTaskId());
            Assert.assertEquals("DecisionContainer taskId must be equal", expected.getProcessId(), actual.getProcessId());
            Assert.assertEquals("DecisionContainer restart times must be the same", expected.getRestartTime(), actual.getRestartTime());
            Assert.assertEquals("ErrorContainer must be the same", expected.getErrorContainer(), actual.getErrorContainer());
            compare(expected.getValue(), actual.getValue());
            if (expected.getTasks() != null) {
                Assert.assertNotNull(actual.getTasks());
                if (actual.getTasks() != null) {
                    Assert.assertEquals("DecisionContainer tasks arrays must have the same length", expected.getTasks().length, actual.getTasks().length);
                    for (int i = 0; i < expected.getTasks().length; i++) {
                        compare(expected.getTasks()[i], actual.getTasks()[i]);
                    }
                }
            } else {
                Assert.assertNull(actual.getTasks());
            }
        } else {
            Assert.assertNull(actual);
        }
    }

}

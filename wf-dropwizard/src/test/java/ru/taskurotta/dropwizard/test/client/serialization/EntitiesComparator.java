package ru.taskurotta.dropwizard.test.client.serialization;

import org.junit.Assert;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.StackTraceElementContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.TaskTarget;

public class EntitiesComparator {

    public static void compare(ArgContainer original, ArgContainer validating) {
        if(original!=null) {
            Assert.assertNotNull(validating);
            if(validating!=null) {
                Assert.assertEquals("Arg class names must be the same", original.getClassName(), validating.getClassName());
                Assert.assertEquals("Arg JSON vakues must be the same", original.getJSONValue(), validating.getJSONValue());
                Assert.assertEquals("Arg task UUID must be the same", original.getTaskId(), validating.getTaskId());
            }
        }
    }

    public static void compare(TaskOptionsContainer original, TaskOptionsContainer validating) {
        if(original!=null) {
            Assert.assertNotNull(validating);
            if(validating != null) {
                //logger.debug("ArgTypes arrays: original[{}], validating[{}]", original, validating);
                Assert.assertArrayEquals("ArgTypes arrays must be the same", original.getArgTypes(), validating.getArgTypes());
            }
        }
    }

    public static void compare(TaskTarget original, TaskTarget validating) {
        if(original != null) {
            Assert.assertNotNull(validating);
            if(validating != null) {
                Assert.assertEquals("Task Target methods must be the same", original.getMethod(), validating.getMethod());
                Assert.assertEquals("Task Target names must be the same", original.getName(), validating.getName());
                Assert.assertEquals("Task Target types must be the same", original.getType(), validating.getType());
                Assert.assertEquals("Task Target versions must be the same", original.getVersion(), validating.getVersion());
            }

        }
    }

    public static void compare(ErrorContainer expected, ErrorContainer actual) {
        if(expected!=null) {
            Assert.assertNotNull(actual);
            if(actual!=null) {
                Assert.assertEquals("ErrorContainer class names must be the same", expected.getClassName(), actual.getClassName());
                Assert.assertEquals("ErrorContainer messages must be the same", expected.getMessage(), actual.getMessage());
                Assert.assertEquals("ErrorContainer restart times must be the same", expected.getRestartTime(), actual.getRestartTime());
                Assert.assertEquals("ErrorContainer should be restarted flags must be the same", expected.isShouldBeRestarted(), actual.isShouldBeRestarted());

                StackTraceElementContainer[] actualStacks = actual.getStackTrace();
                StackTraceElementContainer[] expectedStacks = expected.getStackTrace();
                if(expectedStacks != null) {
                    Assert.assertNotNull(actualStacks);
                    if(actualStacks!=null) {
                        Assert.assertEquals("Stack trace arrays must have the same length", expectedStacks.length, actualStacks.length);
                        for(int i = 0; i<expectedStacks.length;i++ ) {
                            compare(expectedStacks[i], actualStacks[i]);
                        }
                    }
                } else {
                    Assert.assertNull(actualStacks);
                }
            }
        } else {
            Assert.assertNull(actual);
        }
    }

    public static void compare(StackTraceElementContainer expected, StackTraceElementContainer actual) {
        if(expected!=null) {
            Assert.assertNotNull(actual);
            if(actual != null) {
                Assert.assertEquals("StackTraceElementContainer declaring classes must be equal", expected.getDeclaringClass(), actual.getDeclaringClass());
                Assert.assertEquals("StackTraceElementContainer file names must be equal", expected.getFileName(), actual.getFileName());
                Assert.assertEquals("StackTraceElementContainer line numbers must be equal", expected.getLineNumber(), actual.getLineNumber());
                Assert.assertEquals("StackTraceElementContainer method names must be equal", expected.getMethodName(), actual.getMethodName());
            }
        } else {
            Assert.assertNull(actual);
        }
    }


}

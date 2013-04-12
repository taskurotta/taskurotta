package ru.taskurotta.dropwizard.test.client.serialization;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.backend.storage.model.ArgContainer;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.ErrorContainer;
import ru.taskurotta.backend.storage.model.StackTraceElementContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.backend.storage.model.TaskOptionsContainer;
import ru.taskurotta.core.TaskTarget;

public class EntitiesComparator {

    private static final Logger logger = LoggerFactory.getLogger(EntitiesComparator.class);

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
        logger.debug("Comparing error containers: expected[{}], actual[{}]", expected, actual);
        if(expected!=null) {
            Assert.assertNotNull(actual);
            if(actual!=null) {
                Assert.assertEquals("ErrorContainer class names must be the same!", expected.getClassName(), actual.getClassName());
                Assert.assertEquals("ErrorContainer messages must be the same", expected.getMessage(), actual.getMessage());

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

    public static void compare(TaskContainer expected, TaskContainer actual) {
        if(expected != null) {
            Assert.assertNotNull(actual);
            if(actual!=null) {
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

            }
        } else {
            Assert.assertNull(actual);
        }
    }

    public static void compare(ArgContainer[] expected, ArgContainer[] actual) {
        if(expected!=null) {
            Assert.assertNotNull(actual);
            if(actual != null) {
                Assert.assertEquals("Args array size must be the same", expected.length, actual.length);
                if(expected.length == actual.length) {
                    for(int i = 0;i<actual.length;i++) {
                        compare(expected[i], actual[i]);
                    }
                }
            }
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

    public static void compare(DecisionContainer expected, DecisionContainer actual) {
        if(expected != null) {
            Assert.assertNotNull(actual);
            if(actual!=null) {
                Assert.assertEquals("DecisionContainer taskId must be equal", expected.getTaskId(), actual.getTaskId());
                Assert.assertEquals("DecisionContainer taskId must be equal", expected.getProcessId(), actual.getProcessId());
                Assert.assertEquals("DecisionContainer restart times must be the same", expected.getRestartTime(), actual.getRestartTime());
                compare(expected.getErrorContainer(), actual.getErrorContainer());
                compare(expected.getValue(), actual.getValue());
                if(expected.getTasks()!=null) {
                    Assert.assertNotNull(actual.getTasks());
                    if(actual.getTasks()!=null) {
                        Assert.assertEquals("DecisionContainer tasks arrays must have the same length", expected.getTasks().length, actual.getTasks().length);
                        for(int i = 0; i<expected.getTasks().length; i++) {
                           compare(expected.getTasks()[i], actual.getTasks()[i]);
                        }
                    }
                } else {
                    Assert.assertNull(actual.getTasks());
                }
            }
        } else {
            Assert.assertNull(actual);
        }
    }



}

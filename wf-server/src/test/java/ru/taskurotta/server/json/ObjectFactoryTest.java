package ru.taskurotta.server.json;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.test.TestTasks;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * User: romario
 * Date: 2/25/13
 * Time: 4:46 PM
 */
public class ObjectFactoryTest {
    protected final static Logger log = LoggerFactory.getLogger(ObjectFactoryTest.class);

    private ObjectFactory objectFactory;

    @Before
    public void setUp() {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void argContainerBoolean() {
        Boolean arg = Boolean.FALSE;
        testInternal(arg);
    }

    @Test
    public void argContainerArrayBoolean() {
        boolean[] arg = new boolean[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = Math.random() > 0.5;
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        boolean[] newArg = (boolean[])objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerArrayInt() {
        int[] arg = new int[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = i;
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        int[] newArg = (int[])objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerArrayByte() {
        byte[] arg = new byte[10];
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        byte[] newArg = (byte[])objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerArrayShort() {
        short[] arg = new short[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = (short)i;
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        short[] newArg = (short[])objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerArrayLong() {
        long[] arg = new long[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = i;
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        long[] newArg = (long[])objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerArrayDouble() {
        double[] arg = new double[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = Math.random();
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        double[] newArg = (double[])objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerArrayPromise() {
        Promise<?>[] arg = new Promise<?>[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = Promise.asPromise(Math.random());
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        Promise<?>[] newArg = (Promise<?>[])objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerListInt() {
        List<Integer> arg = new ArrayList<Integer>(10);
        for (int i = 0; i < 10; i++) {
            arg.add(i);
        }

        testInternal(arg);
    }

    @Test
    public void argContainerLinkedListInt() {
        List<Integer> arg = new LinkedList<Integer>();
        for (int i = 0; i < 10; i++) {
            arg.add(i);
        }

        testInternal(arg);
    }

    //FixMe: it fails
    @org.junit.Ignore
    @Test
    public void argContainerMapInt() {
        Map<Integer, Object> arg = new HashMap<Integer, Object>();
        Promise payload = Promise.asPromise(true);
        for (int i = 0; i < 10; i++) {
            arg.put(i, payload);
        }

        testInternal(arg);
    }

    @Test
    public void argContainerReadyPromise() {
        Promise arg = Promise.asPromise(Boolean.TRUE);
        testInternal(arg);
    }

    @Test
    public void argContainerNotReadyPromise() {
        Promise arg = Promise.createInstance(UUID.randomUUID());
        testInternal(arg);
    }

    @Test
    public void argContainerWithNull() {
        testInternal(null);
    }

    private void testInternal(Object arg) {
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        Object newArg = objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        assertEquals(arg, newArg);
    }

    @Test
    public void taskSimple() {
        Task task =
                TestTasks.newInstance(
                        UUID.randomUUID(),
                        new TaskTargetImpl(TaskType.DECIDER_START, "ru.example.Decider", "1.0", "start"),
                        new Object[]{true, "Hello!", 10});


        TaskContainer taskContainer = objectFactory.dumpTask(task);

        Task newTask = objectFactory.parseTask(taskContainer);

        assertEquals(task, newTask);

    }

    // TODO
    @Test
    public void resultContainerSimple() {
//        Task task = new Task[]{
//                TestTasks.newInstance(
//                        UUID.randomUUID(),
//                        new TaskTargetImpl(TaskType.DECIDER_START, "ru.example.Decider", "1.0", "start"),
//                        new Object[]{true, "Hello!", 10})
//        };
//
//        TaskDecision taskDecision = new TaskDecisionImpl(UUID.randomUUID(), UUID.randomUUID(), Boolean.TRUE, tasks);
//
//        DecisionContainer decisionContainer = objectFactory.dumpResult(taskDecision);
//
//        System.err.println("decisionContainer = " + decisionContainer);
//
//        TaskDecision newTaskDecision = objectFactory.parseResult(decisionContainer);
//
//        System.err.println("newTaskDecision = " + newTaskDecision);
//
//        assertEquals(taskDecision, newTaskDecision);

    }

    // TODO: test dupm of DecisionContainer

    @Test
    public void testDumpError() {

        class TestExceptions {

            int start(int a, int b) {
                return sum(a, b);
            }

            int sum(int a, int b) {
                int result;

                try {
                    result = a + product(a, b);
                } catch (Exception e) {
                    throw new IllegalArgumentException("3 descendant exception", e);
                }

                return result;
            }

            int product(int a, int b) {
                int result;

                try {
                    result = division(a, b) * b;
                } catch (Exception e) {
                    throw new IllegalArgumentException("2 descendant exception", e);
                }

                return result;
            }

            int division(int a, int b) {
                int result;

                try {
                    result = a / b;
                } catch (Exception e) {
                    throw new IllegalArgumentException("1 descendant exception", e);
                }

                return result;
            }
        }

        try {
            TestExceptions test = new TestExceptions();
            test.start(1, 0);
        } catch (Throwable e) {
            ErrorContainer errorContainer = new ErrorContainer(e);

            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));

            assertEquals(e.getClass().getName(), errorContainer.getClassName());
            assertEquals(e.getMessage(), errorContainer.getMessage());
            assertEquals(writer.toString(), errorContainer.getStackTrace());
        }

    }
}

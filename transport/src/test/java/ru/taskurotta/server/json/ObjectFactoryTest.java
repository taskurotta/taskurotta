package ru.taskurotta.server.json;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.core.*;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.test.TestTasks;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.ErrorContainer;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

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
    public void argContainerPromiseWithFail() {
        Fail fail = new Fail(IllegalArgumentException.class.getName(), "Test error text");
        Promise failedPromiseValue = Promise.asPromise(null);
        ArgContainer argContainer = objectFactory.dumpArg(failedPromiseValue);
        argContainer.setErrorContainer(objectFactory.dumpError(fail));
        //argContainer.setValueType(ArgContainer.ValueType.PLAIN);
        log.info("Straight failPromise[{}]", argContainer);

        Object v = objectFactory.parseArg(argContainer);
        Assert.assertEquals(v, failedPromiseValue);

    }

    @Test
    public void argContainerPromiseWithNull() {
        Object value = Promise.asPromise(null);

        ArgContainer argContainer = objectFactory.dumpArg(value);
        Object v = objectFactory.parseArg(argContainer);

        Assert.assertEquals(v, value);
    }

    @Test
    public void argContainerArrayBoolean() {
        boolean[] arg = new boolean[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = Math.random() > 0.5;
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        boolean[] newArg = (boolean[]) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            Assert.assertEquals(arg[i], newArg[i]);
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

        int[] newArg = (int[]) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            Assert.assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerArrayByte() {
        byte[] arg = new byte[10];
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        byte[] newArg = (byte[]) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            Assert.assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerArrayShort() {
        short[] arg = new short[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = (short) i;
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        short[] newArg = (short[]) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            Assert.assertEquals(arg[i], newArg[i]);
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

        long[] newArg = (long[]) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            Assert.assertEquals(arg[i], newArg[i]);
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

        double[] newArg = (double[]) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            Assert.assertEquals(arg[i], newArg[i], 0.0d);
        }
    }

    @Test
    public void argContainerArrayObject() {
        TestObject[] arg = new TestObject[10];
        // first element would be null
        for (int i = 1; i < arg.length; i++) {
            TestObject testObject = new TestObject("Test object " + i, i);
            arg[i] = testObject;
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        TestObject[] newArg = (TestObject[]) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            Assert.assertEquals(arg[i], newArg[i]);
        }
    }

    // Promise not supported inside object structures except Collections
    @Ignore
    @Test
    public void argContainerArrayPromise() {
        Promise<?>[] arg = new Promise<?>[10];
        for (int i = 0; i < arg.length; i++) {
            arg[i] = Promise.asPromise(Math.random());
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        Promise<?>[] newArg = (Promise<?>[]) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.length, newArg.length);
        for (int i = 0; i < newArg.length; i++) {
            Assert.assertEquals(arg[i], newArg[i]);
        }
    }

    @Test
    public void argContainerCollectionOfString() {
        List<String> arg = new ArrayList<String>();
        arg.add("test1");
        arg.add("test2");
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("Object [{}] dump to [{}]", arg, argContainer);

        List<String> newArg = (List<String>) objectFactory.parseArg(argContainer);
        log.debug("ArgContainer [{}] parsed to [{}]", argContainer, newArg);

        Assert.assertEquals(arg.size(), newArg.size());
        for (int i = 0; i < newArg.size(); i++) {
            Assert.assertEquals(arg.get(i), newArg.get(i));
        }
    }

    @Test
    public void argContainerEmptyCollection() {
        List<String> arg = new ArrayList<String>();
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("Object [{}] dump to [{}]", arg, argContainer);

        List<String> newArg = (List<String>) objectFactory.parseArg(argContainer);
        log.debug("ArgContainer [{}] parsed to [{}]", argContainer, newArg);

        Assert.assertEquals(arg.size(), newArg.size());
        for (int i = 0; i < newArg.size(); i++) {
            Assert.assertEquals(arg.get(i), newArg.get(i));
        }
    }

    @Test
    public void argContainerNullCollection() {
        List<String> arg = null;
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("Object [{}] dump to [{}]", arg, argContainer);

        List<String> newArg = (List<String>) objectFactory.parseArg(argContainer);
        log.debug("ArgContainer [{}] parsed to [{}]", argContainer, newArg);

        Assert.assertEquals(arg, newArg);
    }

    @Test
    public void argContainerCollectionPromise() {
        List<Promise<?>> arg = new ArrayList<Promise<?>>();
        for (int i = 0; i < 10; i++) {
            arg.add(Promise.asPromise(Math.random()));
        }
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        List<Promise<?>> newArg = (List<Promise<?>>) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg.size(), newArg.size());
        for (int i = 0; i < newArg.size(); i++) {
            Assert.assertEquals(arg.get(i), newArg.get(i));
        }
    }

    @Test
    public void argContainerPromiseOfArray() {
        String[] stringArray = new String[10];
        for (int i = 0; i < stringArray.length; i++) {
            stringArray[i] = String.valueOf(Math.random());
        }
        Promise<String[]> target = Promise.asPromise(stringArray);

        ArgContainer argContainer = objectFactory.dumpArg(target);
        log.debug("argContainer = {}", argContainer);

        Promise<String[]> newArg = (Promise<String[]>) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(target.get().length, newArg.get().length);
        for (int i = 0; i < newArg.get().length; i++) {
            Assert.assertEquals(target.get()[i], newArg.get()[i]);
        }
    }

    @Test
    public void argContainerPromiseCollection() {
        List<Promise> target = new ArrayList<Promise>();
        TestObject[] array = getTestObjectArray(10);
        List<TestObject> list = getTestObjectList(10);
        target.add(Promise.asPromise(array));
        target.add(Promise.asPromise(list));

        ArgContainer arg = objectFactory.dumpArg(target);
        List<Promise> newTarget = (List<Promise>) objectFactory.parseArg(arg);

        Assert.assertEquals("Deserialized collection arrays must be the same", target.size(), newTarget.size());
        TestObject[] newArray = (TestObject[]) newTarget.get(0).get();
        List<TestObject> newList = (List<TestObject>) newTarget.get(1).get();
        Assert.assertEquals(newArray.length, array.length);
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(array[i], newArray[i]);
        }

        Assert.assertEquals(list.size(), newList.size());
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(list.get(i), newList.get(i));
        }
    }

    private TestObject[] getTestObjectArray(int size) {
        TestObject[] result = new TestObject[size];
        for (int i = 0; i < size; i++) {
            result[i] = new TestObject("obj-" + 1, Long.valueOf(i));
        }
        return result;
    }

    private List<TestObject> getTestObjectList(int size) {
        List<TestObject> result = new ArrayList<TestObject>();
        for (int i = 0; i < size; i++) {
            result.add(new TestObject("OBJ-" + i, Long.valueOf(i)));
        }
        return result;
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
    public void argContainerListObject() {
        List<TestObject> arg = new ArrayList<TestObject>(10);
        for (int i = 0; i < 10; i++) {
            TestObject testObject = new TestObject("Test object " + i, i);
            arg.add(testObject);
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

    //Promise not supported inside object structures except Collections
    @org.junit.Ignore
    @Test
    public void argContainerMapPromise() {
        Map<Integer, Object> arg = new HashMap<Integer, Object>();
        Promise payload = Promise.asPromise(true);
        for (int i = 0; i < 10; i++) {
            arg.put(i, payload);
        }

        testInternal(arg);
    }

    @Test
    public void argContainerReadyPromiseBoolean() {
        Promise arg = Promise.asPromise(Boolean.TRUE);
        testInternal(arg);
    }

    @Test
    public void argContainerReadyPromiseLong() {
        Promise arg = Promise.asPromise(10L);
        testInternal(arg);
    }

    @Test
    public void argContainerReadyPromiseObject() {
        Promise arg = Promise.asPromise(new TestObject("test", 10));
        testInternal(arg);
    }

    //FixMe: it fails
    @org.junit.Ignore
    @Test
    public void argContainerReadyPromiseArrayInt() {
        int[] value = new int[2];
        value[0] = 1;
        value[1] = 10;

        Promise arg = Promise.asPromise(value);
        ArgContainer argContainer = objectFactory.dumpArg(arg);
        log.debug("argContainer = {}", argContainer);

        Promise<?> newArg = (Promise<?>) objectFactory.parseArg(argContainer);
        log.debug("newArg = {}", newArg);

        Assert.assertEquals(arg, newArg);
/*
        for (int i = 0; i < newArg.length; i++) {
            assertEquals(arg[i], newArg[i]);
        }
*/
    }

    //FixMe: it fails
    @org.junit.Ignore
    @Test
    public void argContainerNestedObjectTest() {
        TestObject child = new TestObject("child", 10);
        ParentObject parent = new ParentObject("parent", child);

        testInternal(parent);
    }


    @Test
    public void argCollectionOfPromiseOfArray() {
        Promise<TestObject[]> pArray = Promise.asPromise(new TestObject[]{new TestObject("child", 10),
                new TestObject("child2", 11)});

        Collection<Promise> coll = new ArrayList<Promise>();
        coll.add(pArray);

        testInternal(coll);
        Assert.assertEquals(((TestObject[]) coll.iterator().next().get())[1].getData(), 11);
    }

    @Test
    public void argCollectionOfPromiseOfList() {
        ArrayList<TestObject> arrayList = new ArrayList<TestObject>();
        arrayList.add(new TestObject("child", 10));
        Promise<List<TestObject>> pList = Promise.asPromise((List<TestObject>) arrayList);

        Promise<TestObject[]> pArray = Promise.asPromise(new TestObject[]{new TestObject("child", 10),
                new TestObject("child2", 11)});

        Collection<Promise> coll = new ArrayList<Promise>();
        coll.add(pList);
        coll.add(pArray);

        testInternal(coll);
        //assertEquals(( (TestObject[]) ((Promise) coll.iterator().next()).get())[1].getData(), 11);
    }

    @Test
    public void argPromiseOfEmptyCollection() {
        ArrayList<TestObject> arrayList = new ArrayList<TestObject>();
        Promise<List<TestObject>> pList = Promise.asPromise((List<TestObject>) arrayList);

        ArgContainer arg = objectFactory.dumpArg(pList);
        Promise<List<TestObject>> pListNew = (Promise<List<TestObject>>) objectFactory.parseArg(arg);
        Assert.assertEquals(pList, pListNew);
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

        Assert.assertEquals(arg, newArg);
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

        Assert.assertEquals(task, newTask);

    }

    @Test
    public void taskWithOptions() {
        Task task =
                TestTasks.newInstance(
                        UUID.randomUUID(),
                        new TaskTargetImpl(TaskType.DECIDER_START, "ru.example.Decider", "1.0", "start"),
                        new Object[]{true, "Hello!", 10},
                        new TaskOptions().setTaskConfig(new TaskConfig().setCustomId("I-AM-CUSTOM-ID")));


        TaskContainer taskContainer = objectFactory.dumpTask(task);

        Task newTask = objectFactory.parseTask(taskContainer);

        Assert.assertEquals(task, newTask);
        Assert.assertEquals("I-AM-CUSTOM-ID", task.getTaskOptions().getTaskConfig().getCustomId());

    }

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

            Assert.assertEquals(e.getClass().getName(), errorContainer.getClassName());
            Assert.assertEquals(e.getMessage(), errorContainer.getMessage());
            Assert.assertEquals(writer.toString(), errorContainer.getStackTrace());
        }

    }
}

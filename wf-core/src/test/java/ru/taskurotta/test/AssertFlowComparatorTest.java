package ru.taskurotta.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;

/**
 * TODO: Test (Promise expectedPromise, Promise interceptedPromise)
 * User: romario
 * Date: 1/24/13
 * Time: 6:14 PM
 */
public class AssertFlowComparatorTest {

    //
    // task1_1 and task1_2 are equals, but have different instances.
    public static Task task1_1;
    public static Task task1_2;
    //
    // task2_1 and task2_2 have equal TaskTarget, but have different arguments
    public static Task task2_1;
    public static Task task2_2;

    public static Promise<Integer> promise1 = Promise.asPromise(111);

    public static Map<UUID, Integer> emptyTaskUuidToSequenceMap = new HashMap<UUID, Integer>();

    static {
        TaskTarget taskTarget1_1 = new TaskTargetImpl(TaskType.DECIDER_START, "taskName", "2.0", "methodName");
        TaskTarget taskTarget1_2 = new TaskTargetImpl(TaskType.DECIDER_START, "taskName", "2.0", "methodName");
        TaskTarget taskTarget2 = new TaskTargetImpl(TaskType.DECIDER_START, "gimme", "1.0", "lollipop");

        Object[] args1_1 = new Object[]{1, "srt"};
        Object[] args1_2 = new Object[]{1, "srt"};
        Object[] args2_1 = new Object[]{10, true, "string"};
        Object[] args2_2 = new Object[]{10, false, "string"};

        task1_1 = TestTasks.newInstance(taskTarget1_1, args1_1);
        task1_2 = TestTasks.newInstance(taskTarget1_2, args1_2);
        task2_1 = TestTasks.newInstance(taskTarget2, args2_1);
        task2_2 = TestTasks.newInstance(taskTarget2, args2_2);
    }


    @Test(expected = TestFailedError.class)
    public void testEqualsWithDifferentSizeOfLists() {

        Task[] expectedTasks = new Task[]{task1_1, task1_1};
        Task[] interceptedTasks = new Task[]{task1_1};

        AssertFlowComparator.assertEquals(expectedTasks, interceptedTasks, promise1, promise1);
    }

    @Test(expected = TestFailedError.class)
    public void testEqualsWithDifferentTaskTargets() {

        Task[] expectedTasks = new Task[]{task1_1};
        Task[] interceptedTasks = new Task[]{task2_1};

        AssertFlowComparator.assertEquals(expectedTasks, interceptedTasks, promise1, promise1);
    }


    @Test()
    public void testEqualsWithEqualsTaskTargets() {

        Task[] expectedTaskList = new Task[]{task1_1};
        Task[] interceptedTaskList = new Task[]{task1_2};

        AssertFlowComparator.assertEquals(expectedTaskList, interceptedTaskList, promise1, promise1);
    }

    @Test(expected = TestFailedError.class)
    public void testEqualsWithDifferentTaskArguments() {

        Task[] expectedTaskList = new Task[]{task2_1};
        Task[] interceptedTaskList = new Task[]{task2_2};

        AssertFlowComparator.assertEquals(expectedTaskList, interceptedTaskList, promise1, promise1);
    }


    @Test(expected = TestFailedError.class)
    public void deepEqualsWithFilledAndEmptyPromises() {
        Promise p1 = Promise.asPromise("Str_value");
        Promise p2 = Promise.createInstance(UUID.randomUUID());

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        AssertFlowComparator.deepEquals(a1, a2, emptyTaskUuidToSequenceMap, emptyTaskUuidToSequenceMap);
    }


    @Test
    public void deepEqualsWithFilledAndEmptyPromises2() {
        UUID uuid = UUID.randomUUID();
        Map<UUID, Integer> taskUuidToSequenceMap = new HashMap<UUID, Integer>();
        taskUuidToSequenceMap.put(uuid, 1);

        Promise p1 = Promise.asPromise("Str_value");
        Promise p2 = Promise.createInstance(uuid);

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        boolean result = AssertFlowComparator.deepEquals(a1, a2, emptyTaskUuidToSequenceMap, taskUuidToSequenceMap);
        assertFalse(result);
    }


    @Test
    public void deepEqualsWithFilledAndEmptyPromises3() {
        UUID uuid = UUID.randomUUID();
        Map<UUID, Integer> taskUuidToSequenceMap = new HashMap<UUID, Integer>();
        taskUuidToSequenceMap.put(uuid, 1);

        Promise p1 = Promise.createInstance(uuid);
        Promise p2 = Promise.asPromise("Str_value");

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        boolean result = AssertFlowComparator.deepEquals(a1, a2, taskUuidToSequenceMap, emptyTaskUuidToSequenceMap);
        assertFalse(result);
    }


    @Test
    public void deepEqualsWithBothNull() {
        Promise p1 = null;
        Promise p2 = null;

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        boolean result = AssertFlowComparator.deepEquals(a1, a2, emptyTaskUuidToSequenceMap, emptyTaskUuidToSequenceMap);
        assertTrue(result);
    }

    @Test
    public void deepEqualsWithFirstNull() {
        Promise p1 = null;
        Promise p2 = Promise.asPromise("Str_value");

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        boolean result = AssertFlowComparator.deepEquals(a1, a2, emptyTaskUuidToSequenceMap, emptyTaskUuidToSequenceMap);
        assertFalse(result);
    }

    @Test
    public void deepEqualsWithSecondNull() {
        Promise p1 = Promise.asPromise("Str_value");
        Promise p2 = null;

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        boolean result = AssertFlowComparator.deepEquals(a1, a2, emptyTaskUuidToSequenceMap, emptyTaskUuidToSequenceMap);
        assertFalse(result);
    }

    @Test
    public void deepEqualsWithFilledPromises() {
        Promise p1 = Promise.asPromise("Str_value");
        Promise p2 = Promise.asPromise("Str_value");

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        boolean result = AssertFlowComparator.deepEquals(a1, a2, emptyTaskUuidToSequenceMap, emptyTaskUuidToSequenceMap);
        assertFalse(!result);
    }


    @Test
    public void deepEqualsWithFilledPromisesWithDifferentArrays() {
        Promise p1 = Promise.asPromise(new Object[]{1, 2});
        Promise p2 = Promise.asPromise(new Object[]{1, 3});

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        boolean result = AssertFlowComparator.deepEquals(a1, a2, emptyTaskUuidToSequenceMap, emptyTaskUuidToSequenceMap);
        assertFalse(result);
    }


    @Test
    public void deepEqualsWithFilledPromisesWithEqualsArrays() {
        Promise p1 = Promise.asPromise(new Object[]{1, 3});
        Promise p2 = Promise.asPromise(new Object[]{1, 3});

        Object[] a1 = new Object[]{1, p1, 2};
        Object[] a2 = new Object[]{1, p2, 2};

        AssertFlowComparator.deepEquals(a1, a2, emptyTaskUuidToSequenceMap, emptyTaskUuidToSequenceMap);
    }


}

package ru.taskurotta.test;

import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * created by void 24.01.13 18:01
 */
public class AssertFlowComparator {

    /**
     * Test two lists of Task arguments
     *
     * @param expectedTasks
     * @param interceptedTasks
     * @return
     */
    public static void assertEquals(Task[] expectedTasks, Task[] interceptedTasks,
                                    Promise expectedPromise, Promise interceptedPromise) {

        // There are two distinguished map, but please notice: Better safe than sorry!
        Map<UUID, Integer> expectedTaskUuidToSequenceMap = new HashMap<UUID, Integer>();
        Map<UUID, Integer> interceptedTaskUuidToSequenceMap = new HashMap<UUID, Integer>();

        if (expectedTasks == null && interceptedTasks != null) {
            throw new TestFailedError(createErrorMessage(expectedTasks, interceptedTasks,
                    "Excepted task list is empty, but intercepted task list are not"));
        }

        if (expectedTasks != null && interceptedTasks == null) {
            throw new TestFailedError(createErrorMessage(expectedTasks, interceptedTasks,
                    "Intercepted task list is empty, but Excepted task list are not"));
        }


        if (expectedTasks != null) {

            if (expectedTasks.length != interceptedTasks.length) {
                throw new TestFailedError(createErrorMessage(expectedTasks, interceptedTasks,
                        "Different size of recorded and expected task lists detected"));
            }

            int sequence = 0;


            // check task lists
            for (int i = 0; i < expectedTasks.length; i++) {

                Task expected = expectedTasks[i];
                Task intercepted = interceptedTasks[i];

                if (!expected.getTarget().equals(intercepted.getTarget())) {
                    throw new TestFailedError("Different taskTarget detected expected:[" + expected + "], intercepted:["
                            + intercepted + "]");
                }

                if (!(deepEquals(expected.getArgs(), intercepted.getArgs(), expectedTaskUuidToSequenceMap, interceptedTaskUuidToSequenceMap))) {
                    throw new TestFailedError("Different task arguments detected expected:[" + expected + "], intercepted:["
                            + intercepted + "]");
                }

                sequence++;

                expectedTaskUuidToSequenceMap.put(expected.getId(), sequence);
                interceptedTaskUuidToSequenceMap.put(intercepted.getId(), sequence);

            }

        }

        // check returned promises

        if (!isPromiseEquals(expectedTaskUuidToSequenceMap, interceptedTaskUuidToSequenceMap,
                expectedPromise, interceptedPromise)) {

            throw new TestFailedError("Returned Promises objects are not equals. intercepted [" + interceptedPromise
                    + "], expected [" + expectedPromise + "].");
        }

    }


    /**
     * Copypasted from Arrays.deepEquals. Added custom Promise comparision.
     *
     * @param a1 one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return <tt>true</tt> if the two arrays are equal
     */
    public static boolean deepEquals(Object[] a1, Object[] a2, Map<UUID, Integer> expectedTaskUuidToSequenceMap,
                                     Map<UUID, Integer> interceptedTaskUuidToSequenceMap) {
        if (a1 == a2)
            return true;
        if (a1 == null || a2 == null)
            return false;
        int length = a1.length;
        if (a2.length != length)
            return false;

        for (int i = 0; i < length; i++) {
            Object e1 = a1[i];
            Object e2 = a2[i];

            if (e1 == e2)
                continue;
            if (e1 == null)
                return false;

            // Figure out whether the two elements are equal
            boolean eq;
            if (e1 instanceof Object[] && e2 instanceof Object[])
                eq = deepEquals((Object[]) e1, (Object[]) e2, expectedTaskUuidToSequenceMap, interceptedTaskUuidToSequenceMap);
            else if (e1 instanceof byte[] && e2 instanceof byte[])
                eq = Arrays.equals((byte[]) e1, (byte[]) e2);
            else if (e1 instanceof short[] && e2 instanceof short[])
                eq = Arrays.equals((short[]) e1, (short[]) e2);
            else if (e1 instanceof int[] && e2 instanceof int[])
                eq = Arrays.equals((int[]) e1, (int[]) e2);
            else if (e1 instanceof long[] && e2 instanceof long[])
                eq = Arrays.equals((long[]) e1, (long[]) e2);
            else if (e1 instanceof char[] && e2 instanceof char[])
                eq = Arrays.equals((char[]) e1, (char[]) e2);
            else if (e1 instanceof float[] && e2 instanceof float[])
                eq = Arrays.equals((float[]) e1, (float[]) e2);
            else if (e1 instanceof double[] && e2 instanceof double[])
                eq = Arrays.equals((double[]) e1, (double[]) e2);
            else if (e1 instanceof boolean[] && e2 instanceof boolean[])
                eq = Arrays.equals((boolean[]) e1, (boolean[]) e2);
            else if (e1 instanceof Promise && e2 instanceof Promise) {

                eq = isPromiseEquals(expectedTaskUuidToSequenceMap, interceptedTaskUuidToSequenceMap, (Promise) e1, (Promise) e2);

            } else
                eq = e1.equals(e2);

            if (!eq)
                return false;
        }

        return true;
    }

    private static boolean isPromiseEquals(Map<UUID, Integer> expectedTaskUuidToSequenceMap, Map<UUID, Integer> interceptedTaskUuidToSequenceMap, Promise p1, Promise p2) {

        boolean eq;

        if (p1 == null && p2 == null) {
            return true;
        }

        if (p1 == null || p2 == null) {
            return false;
        }

        Integer intId1 = expectedTaskUuidToSequenceMap.get(p1.getId());
        Integer intId2 = interceptedTaskUuidToSequenceMap.get(p2.getId());

        if (intId1 == null) {
            if (intId2 == null) {

                Object pValue1;
                Object pValue2;

                try {
                    pValue1 = p1.get();
                    pValue2 = p2.get();
                } catch (IllegalStateException ex) {
                    throw new TestFailedError("One of the Promise not ready yet!", ex);
                }

                if (pValue1 instanceof Object[] && pValue2 instanceof Object[]) {
                    eq = Arrays.deepEquals((Object[]) pValue1, (Object[]) pValue2);
                } else {
                    eq = pValue1.equals(pValue2);
                }

            } else {
                eq = false;
            }
        } else {
            eq = intId2 != null && intId1.equals(intId2);
        }
        return eq;
    }


    /**
     * create error message with detailed list of tasks
     *
     * @param expectedTasks -
     * @param executedTasks -
     * @param errorMessage  -
     */
    public static String createErrorMessage(Task[] expectedTasks, Task[] executedTasks, String errorMessage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);

        printStream.println(errorMessage);
        printTaskLists(printStream, "Expected tasks:", expectedTasks);
        printTaskLists(printStream, "Executed tasks:", executedTasks);

        return baos.toString();
    }

    public static void printTaskLists(PrintStream printStream, String message, Task[] tasks) {
        printStream.println(message);

        if (tasks == null) {
            return;
        }

        for (Task task : tasks) {
            printStream.print("\t");
            printStream.println(task);
        }
    }

}

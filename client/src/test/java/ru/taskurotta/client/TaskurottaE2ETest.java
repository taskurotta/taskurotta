package ru.taskurotta.client;

/**
 */
public class TaskurottaE2ETest {
    /**

    @Test
    public void testProcessInMemory() {

        Taskurotta taskurotta = new Taskurotta();

        // create start process proxy
        MyDecider decider = taskurotta.createDecider(MyDecider.class);
        MyDeciderImpl deciderImpl = null;
        MyWorkerImpl workerImpl = null;

        // set all ClientWorker and self asynchronous interfaces
        taskurotta.prepareDecider(deciderImpl);

        // start process
        decider.start("xxx", "yyy", 10);

        // poll and release one tasks for each invocation.
        Object result1 = taskurotta.runTask(deciderImpl);
        Object result2 = taskurotta.runTask(workerImpl);
        Object result3 = taskurotta.runTask(deciderImpl);

    }

    @Test
    public void testProcessInMemoryAndRemoteWorkerOrSubprocess() {

        Taskurotta taskurotta = new Taskurotta("http://taskurotta.server:8810");

        // create start process proxy
        MyDecider decider = taskurotta.createDecider(MyDecider.class);
        MyDeciderImpl deciderImpl = null;

        // set all ClientWorker and self asynchronous interfaces
        taskurotta.prepareDecider(deciderImpl);

        // start process
        decider.start("xxx", "yyy", 10);

        // poll and release one tasks for each invocation
        Object result1 = taskurotta.runTask(deciderImpl);
        // remote worker should release its task
//        Object result2 = taskurotta.runTask(workerImpl);
        // Wait until timeout.
        Object result3 = taskurotta.runTask(deciderImpl);

    }

    @Test
    public void testWorker() {

        Taskurotta taskurotta = new Taskurotta();

        MyWorkerImpl workerImpl = null;

        // create mock process with start arguments ("xxx", "yyy", 10)
        // poll and release start task with worker task in decision and self task with worker Promise
        // poll and release worker task
        // poll and release mock decider task with worker resolved Promise
        // return worker Promise value
        Object result1 = taskurotta.testWorker(MyWorker.class, workerImpl, "xxx", "yyy", 10);
    }


    @Test
    public void testRemoteWorker() {

        Taskurotta taskurotta = new Taskurotta("http://taskurotta.server:8810");

        MyWorkerImpl workerImpl = null;

        // create mock process with start arguments ("xxx", "yyy", 10)
        // poll and release start task with worker task in decision and self task with worker its Promise
        // remote worker should poll and release its task
        // poll and release mock decider task with worker resolved Promise
        // return worker Promise value
        Object result1 = taskurotta.testRemoteWorker(MyWorker.class, "xxx", "yyy", 10);
    }


    @Test
    public void testRemoteDecider() {

        Taskurotta taskurotta = new Taskurotta("http://taskurotta.server:8810");

        // create mock process with start arguments ("xxx", "yyy", 10)
        // poll and release start task with sub process decider task in decision and self task with worker its Promise
        // remote decider should poll and release its task
        // poll and release mock decider task with worker resolved Promise
        // return worker Promise value
        Object result = taskurotta.testRemoteDecider(MyDecider.class, "xxx", "yyy", 10);
    }

    **/
}

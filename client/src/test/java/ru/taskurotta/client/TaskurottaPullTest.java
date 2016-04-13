package ru.taskurotta.client;

/**
 */
public class TaskurottaPullTest {
/**
    @Test
    public void testSynchronousWorker() {

        Taskurotta taskurotta = new Taskurotta("http://taskurotta.server:8810");

        final ClientNotificationPusher pusher =...;

        MyWorkerImpl workerImpl = new MyWorkerImpl(
                new MyCallback() {
                    void newEvent(FinishedApplication application) throws Exception {
                        pusher.push(application.getUserId(), application.getEventId());
                    }
                });

        // poll and release all tasks until taskurotta.shutdown()
        Object result1 = taskurotta.startWorker(workerImpl);
    }


    @Test
    public void testAsynchronousWorker() {

        Taskurotta taskurotta = new Taskurotta("http://taskurotta.server:8810");

        Task task = taskurotta.pollTask(ActorDefinition actorDef);

        // make some work

        Decision decision = ...;
        taskurotta.releaseTask(task, decision);

    }
    **/
}

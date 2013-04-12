package ru.taskurotta.test;

import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskType;

import java.util.UUID;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 11:05 PM
 */
public abstract class AssertFlow {

    // TODO: Reuse enterprise solution instead of own bicycle!
    public static class Box<A> {
        private A obj;

        public void set(A obj) {
            this.obj = obj;
        }

        public A get() {
            return obj;
        }
    }

    public AssertFlow(RuntimeProcessor runtimeProcessor) {

        // record flow
        // ===================

        final Box<Promise> box = new Box<Promise>();

        UUID processId = UUID.randomUUID();

        Task[] expectedTaskList = runtimeProcessor.execute(processId, new Runnable() {
            @Override
            public void run() {
                Promise promise = expectedFlow();
                box.set(promise);
            }
        });

        Promise expectedPromise = box.get();

        // execute
        // 1. get first tasks
        // 2. run all task sequentially
        // ============================

        Task[] startTasks = runtimeProcessor.execute(processId, new Runnable() {
            @Override
            public void run() {
                execute();
            }
        });


        if (startTasks.length == 0 || startTasks.length > 1) {
            throw new TestFailedError("Decider can have only one start method");
        }

        Task startTask = startTasks[0];

        if (startTask.getTarget().getType() != TaskType.DECIDER_START &&
                startTask.getTarget().getType() != TaskType.DECIDER_ASYNCHRONOUS) {

            throw new TestFailedError("Only DECIDER_START and DECIDER_ASYNCHRONOUS types of Task supported!");
        }

        TaskDecision taskDecision = runtimeProcessor.execute(startTasks[0]);

        Task[] interceptedTasks = taskDecision.getTasks();
        Promise interceptedPromise = (Promise) taskDecision.getValue();

        // compare results
        // ============================


        AssertFlowComparator.assertEquals(expectedTaskList, interceptedTasks, expectedPromise, interceptedPromise);
    }

    public abstract void execute();

    public abstract Promise<?> expectedFlow();

}

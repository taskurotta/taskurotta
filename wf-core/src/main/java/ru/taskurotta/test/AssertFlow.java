package ru.taskurotta.test;

import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.core.TaskType;

import java.util.Arrays;
import java.util.List;

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

        List<Task> expectedTaskList = runtimeProcessor.execute(new Runnable() {
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

        List<Task> startTaskList = runtimeProcessor.execute(new Runnable() {
            @Override
            public void run() {
                execute();
            }
        });


        if (startTaskList.size() == 0 || startTaskList.size() > 1) {
            throw new TestFailedError("Decider can have only one start method");
        }

        Task startTask = startTaskList.get(0);

        if (startTask.getTarget().getType() != TaskType.DECIDER_START &&
                startTask.getTarget().getType() != TaskType.DECIDER_ASYNCHRONOUS) {

            throw new TestFailedError("Only DECIDER_START and DECIDER_ASYNCHRONOUS types of Task supported!");
        }

        TaskDecision taskDecision = runtimeProcessor.execute(startTaskList.get(0));

        Task[] tasks = taskDecision.getTasks();
        List<Task> interceptedTaskList = (tasks == null) ? null : Arrays.asList(tasks);
        Promise interceptedPromise = (Promise) taskDecision.getValue();

        // compare results
        // ============================


        AssertFlowComparator.assertEquals(expectedTaskList, interceptedTaskList, expectedPromise, interceptedPromise);
    }

    public abstract void execute();

    public abstract Promise<?> expectedFlow();

}

package ru.taskurotta.recipes.custom.deciders;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.ActorSchedulingOptions;
import ru.taskurotta.core.Promise;
import ru.taskurotta.recipes.custom.workers.CustomWorkerClient;

/**
 * User: stukushin
 * Date: 15.04.13
 * Time: 19:12
 */
public class CustomDeciderImpl implements CustomDecider {

    private CustomWorkerClient customWorker;
    private DescendantCustomDeciderClient descendantCustomDecider;
    private CustomDeciderImpl asynchronous;

    @Override
    public void calculate(int a, int b) {

        ActorSchedulingOptions workerActorSchedulingOptions = ActorSchedulingOptions.builder().withTaskList("workerTaskList").build();  //new ActorSchedulingOptionsImpl(null, 0l, "workerTaskList");
        ActorSchedulingOptions deciderActorSchedulingOptions = ActorSchedulingOptions.builder().withTaskList("deciderTaskList").build(); //new ActorSchedulingOptionsImpl(null, 0l, "deciderTaskList");
        Promise<?>[] waitFor = new Promise[0];

        Promise<Integer> sum0 = customWorker.sum(a, b);
        asynchronous.show(a, b, sum0, "Invoke worker");

        Promise<Integer> sum1 = customWorker.sum(a, b, workerActorSchedulingOptions);
        asynchronous.show(a, b, sum1, "Invoke worker with ActorSchedulingOptions");

        Promise<Integer> sum2 = customWorker.sum(a, b, waitFor);
        asynchronous.show(a, b, sum2, "Invoke worker with Promise<?> ... waitFor");

        Promise<Integer> sum3 = customWorker.sum(a, b, workerActorSchedulingOptions, waitFor);
        asynchronous.show(a, b, sum3, "Invoke worker with ActorSchedulingOptions and Promise<?> ... waitFor");

        Promise<Integer> sum4 = descendantCustomDecider.calculate(a, b);
        asynchronous.show(a, b, sum4, "Invoke descendant decider");

        Promise<Integer> sum5 = descendantCustomDecider.calculate(a, b, deciderActorSchedulingOptions);
        asynchronous.show(a, b, sum5, "Invoke descendant decider with ActorSchedulingOptions");

        Promise<Integer> sum6 = descendantCustomDecider.calculate(a, b, waitFor);
        asynchronous.show(a, b, sum6, "Invoke descendant decider with Promise<?> ... waitFor");

        Promise<Integer> sum7 = descendantCustomDecider.calculate(a, b, deciderActorSchedulingOptions, waitFor);
        asynchronous.show(a, b, sum7, "Invoke descendant decider with ActorSchedulingOptions and Promise<?> ... waitFor");
    }

    @Asynchronous
    public void show(int a, int b, Promise<Integer> sum, String message) {
        System.out.println(message + ": " + a + " + " + b + " = " + sum.get());
    }

    public void setCustomWorker(CustomWorkerClient customWorker) {
        this.customWorker = customWorker;
    }

    public void setDescendantCustomDecider(DescendantCustomDeciderClient descendantCustomDecider) {
        this.descendantCustomDecider = descendantCustomDecider;
    }

    public void setAsynchronous(CustomDeciderImpl asynchronous) {
        this.asynchronous = asynchronous;
    }
}

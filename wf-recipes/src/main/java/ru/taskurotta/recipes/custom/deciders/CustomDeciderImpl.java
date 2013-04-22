package ru.taskurotta.recipes.custom.deciders;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.ActorSchedulingOptions;
import ru.taskurotta.core.Promise;
import ru.taskurotta.internal.core.ActorSchedulingOptionsImpl;
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
        ActorSchedulingOptions actorSchedulingOptions = new ActorSchedulingOptionsImpl();
        Promise<?>[] waitFor = new Promise[0];

        Promise<Integer> sum0 = customWorker.sum(a, b);
        asynchronous.show(a, b, sum0, "Simple invoke");

        Promise<Integer> sum1 = customWorker.sum(a, b, actorSchedulingOptions);
        asynchronous.show(a, b, sum1, "Invoke with ActorSchedulingOptions");

        Promise<Integer> sum2 = customWorker.sum(a, b, waitFor);
        asynchronous.show(a, b, sum2, "Invoke with Promise<?> ... waitFor");

        Promise<Integer> sum3 = customWorker.sum(a, b, actorSchedulingOptions, waitFor);
        asynchronous.show(a, b, sum3, "Invoke with ActorSchedulingOptions and Promise<?> ... waitFor");

        Promise<Integer> sum4 = descendantCustomDecider.calculate(a, b);
        asynchronous.show(a, b, sum4, "Simple invoke descendant decider");

        Promise<Integer> sum5 = descendantCustomDecider.calculate(a, b, actorSchedulingOptions);
        asynchronous.show(a, b, sum5, "Invoke descendant decider with ActorSchedulingOptions");

        Promise<Integer> sum6 = descendantCustomDecider.calculate(a, b, waitFor);
        asynchronous.show(a, b, sum6, "Invoke descendant decider with Promise<?> ... waitFor");

        Promise<Integer> sum7 = descendantCustomDecider.calculate(a, b, actorSchedulingOptions, waitFor);
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

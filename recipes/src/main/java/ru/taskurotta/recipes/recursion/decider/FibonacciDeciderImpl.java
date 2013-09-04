package ru.taskurotta.recipes.recursion.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;

/**
 * User: stukushin
 * Date: 19.03.13
 * Time: 12:48
 */
public class FibonacciDeciderImpl implements FibonacciDecider {
    Logger logger = LoggerFactory.getLogger(getClass());

    protected FibonacciDeciderImpl asynchronous;

    @Override
    public Promise<Integer> calculate(int n) {
        Promise<Integer> fibonacci = asynchronous.fibonacci(n);

        asynchronous.show(n, fibonacci);

        return fibonacci;
    }

    @Asynchronous
    public Promise<Integer> fibonacci(int n) {
        if (n > 2) {
            int n1 = n - 1;
            int n2 = n - 2;

            Promise<Integer> recN1 = asynchronous.fibonacci(n1);
            Promise<Integer> recN2 = asynchronous.fibonacci(n2);

            return asynchronous.waitFibonacciResult(recN1, recN2);
        } else {
            return Promise.asPromise(1);
        }
    }


    //ToDo (stukushin) : Разобраться, почему нельзя использовать приватные методы с asynchronous.
    @Asynchronous
    public Promise<Integer> waitFibonacciResult(Promise<Integer> recN1, Promise<Integer> recN2) {
        return Promise.asPromise(recN1.get() + recN2.get());
    }

    @Asynchronous
    public void show(int n, Promise<Integer> fibonacci) {
        logger.info("Fibonacci number at position [{}] is [{}]", n, fibonacci.get());
    }

    public void setAsynchronous(FibonacciDeciderImpl asynchronous) {
        this.asynchronous = asynchronous;
    }
}

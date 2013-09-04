package ru.taskurotta;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.DeciderClient;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.test.AssertFlow;

/**
 * User: romario
 * Date: 1/22/13
 * Time: 10:38 PM
 */
public class ExecuteDeciderTest {

    // worker client
    // =================

    @Worker
    public static interface SimpleWorker {
        public int max(int a, int b);
    }

    public static class SimpleWorkerImpl implements SimpleWorker {

        @Override
        public int max(int a, int b) {
            return Math.max(a, b);
        }
    }

    @WorkerClient(worker = SimpleWorker.class)
    public static interface SimpleWorkerClient {
        public Promise<Integer> max(int a, int b);
    }


    // decider client
    // =================

    @Decider
    public static interface SimpleSubDecider {

        @Execute
        public void startIt(String str);
    }

    @DeciderClient(decider = SimpleSubDecider.class)
    public static interface SimpleSubDeciderClient {

        public Promise<Void> startIt(String str);
    }


    // decider
    // ==================

    @Decider
    public static interface SimpleDecider {

        @Execute
        public void start();
    }

    public static class SimpleDeciderImpl implements SimpleDecider {

        protected SimpleDeciderImpl async;

        protected SimpleWorkerClient simpleWorkerClient;

        protected SimpleSubDeciderClient simpleSubDeciderClient;

        @Override
        public void start() {

            Promise<Integer> maxValue = simpleWorkerClient.max(10, 55);
            async.startSubDecider(async.convertToString(maxValue));
            simpleSubDeciderClient.startIt("abc");
        }

        @Asynchronous
        public Promise<String> convertToString(Promise<Integer> maxValue) {
            return Promise.asPromise(maxValue.get().toString());
        }

        @Asynchronous
        public void startSubDecider(Promise<String> str) {
            simpleSubDeciderClient.startIt(str.get());
        }

    }

    @Decider
    public static interface SimpleDeciderWithAsynchronousMethod {

        @Execute
        public void start(int a, int b);
    }

    public static class SimpleDeciderWithAsynchronousMethodImpl implements SimpleDeciderWithAsynchronousMethod {

        protected SimpleDeciderWithAsynchronousMethodImpl async;

        @Override
        public void start(int a, int b) {
            async.max(a, b);
        }

        @Asynchronous
        public Promise<Integer> max(int a, int b) {
            return Promise.asPromise(Math.max(a, b));
        }
    }

    @Decider
    public static interface SimpleDeciderStartMethodReturnPromise {

        @Execute
        public Promise<Integer> start(int a, int b);
    }

    public static class SimpleDeciderStartMethodReturnPromiseImpl implements SimpleDeciderStartMethodReturnPromise {

        protected SimpleWorkerClient simpleWorkerClient;

        @Override
        public Promise<Integer> start(int a, int b) {
            return simpleWorkerClient.max(a, b);
        }
    }


    @Decider
    public static interface FibonacciDecider {

        @Execute
        public Promise<Integer> start(int n);
    }

    public static class FibonacciDeciderImpl implements FibonacciDecider {

        protected FibonacciDeciderImpl async;

        @Override
        public Promise<Integer> start(int n) {
            return async.fibonacci(n);
        }

        @Asynchronous
        public Promise<Integer> fibonacci(int n) {
            int n1 = n - 1;
            int n2 = n - 2;

            Promise<Integer> recN1 = async.fibonacci(n1);
            Promise<Integer> recN2 = async.fibonacci(n2);

            return async.waitFibonacciResult(n, recN1, recN2);
        }

        @Asynchronous
        public Promise<Integer> waitFibonacciResult(int n, Promise<Integer> recN1, Promise<Integer> recN2) {
            return n <= 2 ? Promise.asPromise(1) : Promise.asPromise(recN1.get() + recN2.get());
        }
    }

    // test initialization
    // ===================


    SimpleWorkerClient simpleWorkerClient;
    SimpleSubDeciderClient simpleSubDeciderClient;
    SimpleDeciderImpl simpleDecider;
    SimpleDeciderWithAsynchronousMethodImpl simpleDeciderWithAsyncMethod;
    FibonacciDeciderImpl fibonacciDecider;
    SimpleDeciderStartMethodReturnPromise simpleDeciderStartMethodReturnPromise;

    RuntimeProcessor runtimeProcessorSimpleDeciderImpl;
    RuntimeProcessor runtimeProcessorSimpleDeciderWithAsynchronousMethodImpl;
    RuntimeProcessor runtimeProcessorFibonacciDeciderImpl;
    RuntimeProcessor runtimeProcessorSimpleDeciderStartMethodReturnPromiseImpl;

    @Before
    public void prepare() {

        RuntimeProvider runtimeProvider = RuntimeProviderManager.getRuntimeProvider();

        SimpleDeciderImpl simpleDeciderImpl = new SimpleDeciderImpl();
        simpleDeciderImpl.async = ProxyFactory.getAsynchronousClient(SimpleDeciderImpl.class);
        simpleDeciderImpl.simpleSubDeciderClient = ProxyFactory.getDeciderClient(SimpleSubDeciderClient.class);
        simpleDeciderImpl.simpleWorkerClient = ProxyFactory.getWorkerClient(SimpleWorkerClient.class);

        runtimeProcessorSimpleDeciderImpl = runtimeProvider.getRuntimeProcessor(simpleDeciderImpl);

        SimpleDeciderWithAsynchronousMethodImpl simpleDeciderWithAsynchronousMethodImpl = new SimpleDeciderWithAsynchronousMethodImpl();
        simpleDeciderWithAsynchronousMethodImpl.async = ProxyFactory.getAsynchronousClient(SimpleDeciderWithAsynchronousMethodImpl.class);

        runtimeProcessorSimpleDeciderWithAsynchronousMethodImpl = runtimeProvider.getRuntimeProcessor(simpleDeciderWithAsynchronousMethodImpl);

        FibonacciDeciderImpl fibonacciDeciderImpl =  new FibonacciDeciderImpl();
        fibonacciDeciderImpl.async = ProxyFactory.getAsynchronousClient(FibonacciDeciderImpl.class);

        runtimeProcessorFibonacciDeciderImpl = runtimeProvider.getRuntimeProcessor(fibonacciDeciderImpl);

        SimpleDeciderStartMethodReturnPromiseImpl simpleDeciderStartMethodReturnPromiseImpl = new SimpleDeciderStartMethodReturnPromiseImpl();
        simpleDeciderStartMethodReturnPromiseImpl.simpleWorkerClient = ProxyFactory.getWorkerClient(SimpleWorkerClient.class);

        runtimeProcessorSimpleDeciderStartMethodReturnPromiseImpl = runtimeProvider.getRuntimeProcessor(simpleDeciderStartMethodReturnPromiseImpl);

        // get references to decider actors
        //==================================

        this.simpleWorkerClient = ProxyFactory.getWorkerClient(SimpleWorkerClient.class);
        this.simpleSubDeciderClient = ProxyFactory.getDeciderClient(SimpleSubDeciderClient.class);
        this.simpleDecider = ProxyFactory.getAsynchronousClient(SimpleDeciderImpl.class);
        this.simpleDeciderWithAsyncMethod = ProxyFactory.getAsynchronousClient(SimpleDeciderWithAsynchronousMethodImpl.class);
        this.fibonacciDecider = ProxyFactory.getAsynchronousClient(FibonacciDeciderImpl.class);
        this.simpleDeciderStartMethodReturnPromise = ProxyFactory.getAsynchronousClient(SimpleDeciderStartMethodReturnPromiseImpl.class);
    }


    // simple test
    // ====================

    @Test
    public void simpleTest() {

        // test decider
        //===================================
        new AssertFlow(runtimeProcessorSimpleDeciderImpl) {

            public void execute() {
                simpleDecider.start();
            }

            public Promise expectedFlow() {
                Promise<Integer> maxValue = simpleWorkerClient.max(10, 55);
                simpleDecider.startSubDecider(simpleDecider.convertToString(maxValue));
                simpleSubDeciderClient.startIt("abc");

                return null;
            }

        };

    }


    @Test
    public void testAsynchronousDeciderMethodWithArgs() {
        final int a = 10;
        final int b = 20;

        new AssertFlow(runtimeProcessorSimpleDeciderWithAsynchronousMethodImpl) {
            public void execute() {
                simpleDeciderWithAsyncMethod.max(a, b);
            }

            public Promise expectedFlow() {
                return Promise.asPromise(20);
            }
        };
    }

    @Test
    public void testRecursiveDecider() {
        final int n = 4;

        new AssertFlow(runtimeProcessorFibonacciDeciderImpl) {

            public void execute() {
                fibonacciDecider.fibonacci(n);
            }

            public Promise expectedFlow() {
                Promise<Integer> recN1 = fibonacciDecider.fibonacci(3);
                Promise<Integer> recN2 = fibonacciDecider.fibonacci(2);

                return fibonacciDecider.waitFibonacciResult(4, recN1, recN2);
            }

        };
    }

    @Test
    public void testDeciderWithStartMethodReturnPromise() {
        final int a = 10;
        final int b = 20;

        new AssertFlow(runtimeProcessorSimpleDeciderStartMethodReturnPromiseImpl) {
            public void execute() {
                simpleDeciderStartMethodReturnPromise.start(a, b);
            }

            public Promise expectedFlow() {
                return simpleWorkerClient.max(a, b);
            }
        };
    }

}

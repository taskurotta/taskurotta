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
public class SupportUnitTest {

    // worker client
    // =================

    @Worker
    public static interface SimpleWorker {
        public int max(int a, int b);
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
        public Promise<String> startIt(String str);
    }

    @DeciderClient(decider = SimpleSubDecider.class)
    public static interface SimpleSubDeciderClient {

        public Promise<String> startIt(String str);
    }


    // decider
    // ==================

    @Decider
    public static interface SimpleTestDecider {

        @Execute
        public Promise<String> start();
    }


    public static class SimpleTestDeciderImpl implements SimpleTestDecider {

        public SimpleTestDeciderImpl async;

        public SimpleWorkerClient simpleWorkerClient;

        public SimpleSubDeciderClient simpleSubDeciderClient;

        @Override
        public Promise<String> start() {
			Promise<Integer> maxValue = simpleWorkerClient.max(10, 55);

            return async.startSubDecider(async.convertToString(maxValue));
        }

        @Asynchronous
        public Promise<String> convertToString(Promise<Integer> maxValue) {
            return Promise.asPromise(maxValue.get().toString());
        }

        @Asynchronous
        public Promise<String> startSubDecider(Promise<String> str) {
            return simpleSubDeciderClient.startIt(str.get());
        }

    }


    // test initialization
    // ===================

    RuntimeProcessor runtimeProcessor;

    SimpleWorkerClient simpleWorkerClient;
    SimpleSubDeciderClient simpleSubDeciderClient;
    SimpleTestDeciderImpl simpleDecider;

    @Before
    public void prepare() {

        RuntimeProvider runtimeProvider = RuntimeProviderManager.getRuntimeProvider();

        SimpleTestDeciderImpl simpleTestDeciderImpl = new SimpleTestDeciderImpl();
        simpleTestDeciderImpl.async = ProxyFactory.getAsynchronousClient(SimpleTestDeciderImpl.class);
        simpleTestDeciderImpl.simpleSubDeciderClient = ProxyFactory.getDeciderClient(SimpleSubDeciderClient.class);
        simpleTestDeciderImpl.simpleWorkerClient = ProxyFactory.getWorkerClient(SimpleWorkerClient.class);

        runtimeProcessor = runtimeProvider.getRuntimeProcessor(simpleTestDeciderImpl);

        // get references to decider actors
        //==================================

        this.simpleWorkerClient = ProxyFactory.getWorkerClient(SimpleWorkerClient.class);
        this.simpleSubDeciderClient = ProxyFactory.getDeciderClient(SimpleSubDeciderClient.class);
        this.simpleDecider = ProxyFactory.getAsynchronousClient(SimpleTestDeciderImpl.class);

    }


    // simple test
    // ====================

    @Test
    public void simpleTest() {

        // test decider
        //===================================
        new AssertFlow(runtimeProcessor) {

            public void execute() {
                simpleDecider.start();
            }

            public Promise expectedFlow() {
                Promise<Integer> maxValue = simpleWorkerClient.max(10, 55);
                Promise<String> str = simpleDecider.convertToString(maxValue);

                return simpleDecider.startSubDecider(str);
            }

        };

    }

	@Test
	public void asyncCallTest() {
		final Promise<Integer> value = Promise.asPromise(55);
		new AssertFlow(runtimeProcessor) {

			public void execute() {
				simpleDecider.convertToString(value);
			}

			public Promise expectedFlow() {
				return Promise.asPromise("55");
			}

		};
	}
}

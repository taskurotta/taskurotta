package ru.taskurotta.internal;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.RuntimeProviderManager;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.Promise;
import ru.taskurotta.exception.IncorrectExecuteMethodDefinition;
import ru.taskurotta.exception.TaskTargetRequiredException;

/**
 * User: stukushin
 * Date: 24.01.13
 * Time: 14:32
 */
public class SWFRuntimeProviderTest {

    RuntimeProvider runtimeProvider;

    // worker client without annotation @Worker
    public static interface SimpleWorkerWithoutAnnotation {
        public int max(int a, int b);
    }

    @WorkerClient(worker = SimpleWorkerWithoutAnnotation.class)
    public static interface SimpleWorkerWithoutAnnotationClient {
        public Promise<Integer> max(int a, int b);
    }

    // decider without annotation @Decider
    public static interface SimpleDeciderWithoutAnnotation {
        @Execute
        public void start();
    }

    // decider with many @Execute annotation
    @Decider
    public static interface SimpleDeciderWithManyExecuteAnnotation {
        @Execute
        public void start();

        @Execute
        public void start1();
    }

    // decider with @Execute method with Promise parameters
    @Decider
    public static interface SimpleDeciderWithExecuteMethodWithPromiseParameters {
        @Execute
        public void start(Promise promise);
    }

    // decider with @Execute method returning something
    @Decider
    public static interface SimpleDeciderWithExecuteMethodReturningSomething {
        @Execute
        public int start();
    }

    // decider without @Execute method
    @Decider
    public static interface SimpleDeciderWithoutExecuteMethod {
    }


    @Before
    public void setUp() throws Exception {
        runtimeProvider = RuntimeProviderManager.getRuntimeProvider();
    }

    @Test(expected = TaskTargetRequiredException.class)
    public void testRegisterIncorrectWorkerTargets() throws Exception {
        runtimeProvider.getRuntimeProcessor(new SimpleWorkerWithoutAnnotationClient() {
            @Override
            public Promise<Integer> max(int a, int b) {
                return null;
            }
        });
    }

    @Test(expected = TaskTargetRequiredException.class)
    public void testRegisterIncorrectDeciderTargets() throws Exception {
        runtimeProvider.getRuntimeProcessor(new SimpleDeciderWithoutAnnotation() {
            @Override
            public void start() {
            }
        });
    }

    @Test(expected = IncorrectExecuteMethodDefinition.class)
    public void testRegisterDeciderMethodsWithManyExecuteAnnotations() {
        runtimeProvider.getRuntimeProcessor(new SimpleDeciderWithManyExecuteAnnotation() {
            @Override
            public void start() {
            }

            @Override
            public void start1() {
            }
        });
    }

    @Test(expected = IncorrectExecuteMethodDefinition.class)
    public void testRegisterDeciderMethodsWithExecuteMethodWithPromiseParameters() {
        runtimeProvider.getRuntimeProcessor(new SimpleDeciderWithExecuteMethodWithPromiseParameters() {
            @Override
            public void start(Promise promise) {
            }
        });
    }

    @Test(expected = IncorrectExecuteMethodDefinition.class)
    public void testRegisterDeciderMethodsWithExecuteMethodReturningSomething() {
        runtimeProvider.getRuntimeProcessor(new SimpleDeciderWithExecuteMethodReturningSomething() {
            @Override
            public int start() {
                return 0;
            }
        });
    }

    @Test(expected = IncorrectExecuteMethodDefinition.class)
    public void testRegisterDeciderMethodsWithoutExecuteMethod() {
        runtimeProvider.getRuntimeProcessor(new SimpleDeciderWithoutExecuteMethod() {
        });
    }
}

package ru.taskurotta.proxy;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.core.Promise;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.internal.core.MethodDescriptor;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.internal.proxy.ProxyInvocationHandler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * User: stukushin
 * Date: 17.01.13
 * Time: 12:19
 */
public class WorkerInvocationHandlerTest {
    private ProxyInvocationHandler workerInvocationHandler;
    private SimpleProxy simpleProxy;
    private Method methodSum;
    private Method methodAVoid;

    @SuppressWarnings("UnusedDeclaration")
    public static class SimpleProxy {

        public Promise<Integer> sum(int a, int b) {
            return Promise.asPromise(a + b);
        }

        public void aVoid() {
            // nothing
        }
    }


    @Before
    public void before() throws NoSuchMethodException {
        simpleProxy = new SimpleProxy();
        Map<Method, MethodDescriptor> method2TaskTargetCache = new HashMap<Method, MethodDescriptor>();

        methodSum = SimpleProxy.class.getMethod("sum", new Class[]{int.class, int.class});
        TaskTargetImpl target = new TaskTargetImpl(TaskType.WORKER, "testName", "1.0", methodSum.getName());
        method2TaskTargetCache.put(methodSum, new MethodDescriptor(target));

        methodAVoid = SimpleProxy.class.getMethod("aVoid", new Class[]{});
        target = new TaskTargetImpl(TaskType.WORKER, "testName", "1.0", methodAVoid.getName());
        method2TaskTargetCache.put(methodAVoid, new MethodDescriptor(target));

        workerInvocationHandler = new ProxyInvocationHandler(method2TaskTargetCache, null);
    }

    @Test
    public void testSum() throws Throwable {

        RuntimeContext.start(UUID.randomUUID());

        try {
            assertEquals(Promise.class, workerInvocationHandler.invoke(simpleProxy, methodSum, new Object[]{1, 2}).getClass());
        } finally {
            RuntimeContext.finish();
        }
    }

    @Test
    public void testAVoid() throws Throwable {
        RuntimeContext.start(UUID.randomUUID());

        try {
            assertNull(workerInvocationHandler.invoke(simpleProxy, methodAVoid, new Object[]{}));
        } finally {
            RuntimeContext.finish();
        }
    }
}

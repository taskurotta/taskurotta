package ru.taskurotta.proxy;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.TaskHandler;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.proxy.ProxyInvocationHandler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
        Map<Method,TaskTarget> method2TaskTargetCache = new HashMap<Method, TaskTarget>();

        methodSum = SimpleProxy.class.getMethod("sum", new Class[]{int.class, int.class});
        method2TaskTargetCache.put(methodSum, new TaskTargetImpl(TaskType.WORKER, "testName", "1.0", methodSum.getName()));

        methodAVoid = SimpleProxy.class.getMethod("aVoid", new Class[]{});
        method2TaskTargetCache.put(methodAVoid, new TaskTargetImpl(TaskType.WORKER, "testName", "1.0", methodAVoid.getName()));

        workerInvocationHandler = new ProxyInvocationHandler(method2TaskTargetCache, null);
    }

    @Test
    public void testSum() throws Throwable {
        assertEquals(Promise.class, workerInvocationHandler.invoke(simpleProxy, methodSum, new Object[]{1, 2}).getClass());
    }

    @Test
    public void testAVoid() throws Throwable {
        assertNull(workerInvocationHandler.invoke(simpleProxy, methodAVoid, new Object[]{}));
    }
}

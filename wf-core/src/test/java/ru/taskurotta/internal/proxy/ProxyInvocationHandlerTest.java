package ru.taskurotta.internal.proxy;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.exception.IllegalReturnTypeException;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.internal.core.MethodDescriptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * User: stukushin
 * Date: 24.01.13
 * Time: 18:54
 */
public class ProxyInvocationHandlerTest {

    private ProxyInvocationHandler proxyInvocationHandler;

    class TestProxy {
        public int incorrectMethod(int a, int b) {
            return a + b;
        }

        public void voidMethod() {
        }

        public Promise<Integer> correctMethod(int a, int b) {
            return Promise.asPromise(a + b);
        }
    }

    @Before
    public void setUp() {
        Class clazz = TestProxy.class;

        Method[] methods = clazz.getMethods();
        Map<Method, MethodDescriptor> method2TaskTargetCache = new HashMap<Method, MethodDescriptor>();
        for (Method method : methods) {
            method2TaskTargetCache.put(method, new MethodDescriptor(TaskType.DECIDER_ASYNCHRONOUS, "testActorName", "1.0", method.getName()));
        }

        proxyInvocationHandler = new ProxyInvocationHandler(method2TaskTargetCache, null);
    }

    @Test
    public void testInvokeCorrectMethod() throws Throwable {
        Class clazz = TestProxy.class;
        Method method = clazz.getMethod("correctMethod", int.class, int.class);

        RuntimeContext.start(UUID.randomUUID());

        try {

            Object object = proxyInvocationHandler.invoke(new TestProxy(), method, new Object[]{1, 2});

            assertSame(object.getClass(), Promise.class);

        } finally {
            RuntimeContext.finish();
        }
    }

    @Test
    public void testInvokeVoidMethod() throws Throwable {
        Class clazz = TestProxy.class;
        Method method = clazz.getMethod("voidMethod");

        RuntimeContext.start(UUID.randomUUID());

        try {

            Object object = proxyInvocationHandler.invoke(new TestProxy(), method, new Object[]{1, 2});

            assertNull(object);

        } finally {
            RuntimeContext.finish();
        }
    }

    @Test(expected = IllegalReturnTypeException.class)
    public void testInvokeIncorrectMethod() throws Throwable {
        Class clazz = TestProxy.class;
        Method method = clazz.getMethod("incorrectMethod", int.class, int.class);

        RuntimeContext.start(UUID.randomUUID());

        try {
            proxyInvocationHandler.invoke(new TestProxy(), method, new Object[]{1, 2});
        } finally {
            RuntimeContext.finish();
        }
    }
}

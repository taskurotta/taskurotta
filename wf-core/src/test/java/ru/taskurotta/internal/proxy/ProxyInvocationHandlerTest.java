package ru.taskurotta.internal.proxy;

import org.junit.Before;
import org.junit.Test;
import ru.taskurotta.TaskHandler;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.exception.IllegalReturnTypeException;
import ru.taskurotta.internal.core.TaskTargetImpl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
        public int incorrectMethod (int a, int b) {
            return a + b;
        }

        public void voidMethod() {}

        public Promise<Integer> correctMethod(int a, int b) {
            return Promise.asPromise(a + b);
        }
    }

    @Before
    public void setUp() {
        Class clazz = TestProxy.class;

        Method[] methods = clazz.getMethods();
        Map<Method, TaskTarget> method2TaskTargetCache = new HashMap<Method, TaskTarget>();
        for (Method method : methods) {
            method2TaskTargetCache.put(method, new TaskTargetImpl(TaskType.DECIDER_ASYNCHRONOUS, "testActorName", "1.0", method.getName()));
        }

        proxyInvocationHandler = new ProxyInvocationHandler(method2TaskTargetCache, new TaskHandler() {
            @Override
            public void handle(Task task) {
            }
        });
    }

    @Test
    public void testInvokeCorrectMethod() throws Throwable {
        Class clazz = TestProxy.class;
        Method method = clazz.getMethod("correctMethod", int.class, int.class);

        Object object = proxyInvocationHandler.invoke(new TestProxy(), method, new Object[]{1, 2});

        assertSame(object.getClass(), Promise.class);
    }

    @Test
    public void testInvokeVoidMethod() throws Throwable {
        Class clazz = TestProxy.class;
        Method method = clazz.getMethod("voidMethod");

        Object object = proxyInvocationHandler.invoke(new TestProxy(), method, new Object[]{1, 2});

        assertNull(object);
    }

    @Test(expected = IllegalReturnTypeException.class)
    public void testInvokeIncorrectMethod() throws Throwable {
        Class clazz = TestProxy.class;
        Method method = clazz.getMethod("incorrectMethod", int.class, int.class);

        proxyInvocationHandler.invoke(new TestProxy(), method, new Object[]{1, 2});
    }
}

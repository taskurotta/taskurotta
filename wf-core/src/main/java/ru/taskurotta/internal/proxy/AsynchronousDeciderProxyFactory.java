package ru.taskurotta.internal.proxy;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import ru.taskurotta.TaskHandler;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.util.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * created by void 23.01.13 12:51
 */
public class AsynchronousDeciderProxyFactory extends CachedProxyFactory {

    @Override
    public <TargetInterface> Object createProxy(Class<TargetInterface> proxyType, TaskHandler taskHandler) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(proxyType);

        final Map<Method, TaskTarget> method2TaskTargetCache = createMethodCache(proxyType);

        ProxyInvocationHandler proxyInvocationHandler = new ProxyInvocationHandler(method2TaskTargetCache, taskHandler);
        Callback[] callbacks = createCallbacks(proxyInvocationHandler);

        enhancer.setCallbacks(callbacks);

        CallbackFilter callbackFilter = new CallbackFilter() {

            private static final int INTERCEPT_TASK = 0;
            private static final int THROW_EXCEPTION = 1;

            @Override
            public int accept(Method method) {

                boolean annotationPresent = method2TaskTargetCache.containsKey(method);

                if (annotationPresent) {
                    return INTERCEPT_TASK;
                }

                return THROW_EXCEPTION;
            }
        };

        enhancer.setCallbackFilter(callbackFilter);

        return enhancer.create();
    }

    private Callback[] createCallbacks(final ProxyInvocationHandler proxyInvocationHandler) {

        Callback allowCallback = new MethodInterceptor() {
            @Override
            public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                return proxyInvocationHandler.invoke(object, method, args);
            }
        };

        Callback disallowCallback = new MethodInterceptor() {

            @Override
            public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

                throw new IllegalAccessError(
                        "Access denied to methods without Asynchronous annotation: "
                                + object.getClass().getName() + "." + method.getName() + "()");
            }
        };

        return new Callback[]{allowCallback, disallowCallback};
    }

    private Map<Method, TaskTarget> createMethodCache(Class target) {
        Map<Method, TaskTarget> method2TaskTargetCache = new HashMap<Method, TaskTarget>();

        Class<?> deciderInterface = AnnotationUtils.findAnnotatedClass(target, Decider.class);

        String deciderName = DeciderProxyFactory.deciderName(deciderInterface);
        String deciderVersion = DeciderProxyFactory.deciderVersion(deciderInterface);

        // find @Asynchronous methods
        Method[] targetMethods = target.getMethods();
        for (Method method : targetMethods) {

            if (method.isAnnotationPresent(Asynchronous.class)) {
                TaskTarget value = new TaskTargetImpl(TaskType.DECIDER_ASYNCHRONOUS, deciderName, deciderVersion, method.getName());
                method2TaskTargetCache.put(method, value);
            }

        }

        /**
         * Find @Execute method
         */
        for (Method method : deciderInterface.getMethods()) {

            Execute executeAnnotation = method.getAnnotation(Execute.class);
            if (null != executeAnnotation) {

                String interfaceMethod = method.getName();

                for (Method implementationMethod : targetMethods) {

                    if (implementationMethod.getName().equals(interfaceMethod)) {
                        TaskTarget value = new TaskTargetImpl(TaskType.DECIDER_START, deciderName, deciderVersion, method.getName());
                        method2TaskTargetCache.put(implementationMethod, value);
                        break;
                    }
                }

                break;
            }

        }


        return method2TaskTargetCache;
    }
}

package ru.taskurotta.internal.proxy;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import ru.taskurotta.annotation.AcceptFail;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.IncorrectAsynchronousMethodDefinition;
import ru.taskurotta.exception.IncorrectExecuteMethodDefinition;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.internal.core.MethodDescriptor;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.util.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * created by void 23.01.13 12:51
 */
public class AsynchronousDeciderProxyFactory extends CachedProxyFactory {

    @Override
    public <TargetInterface> TargetInterface createProxy(Class<TargetInterface> proxyType,
                                                RuntimeContext injectedRuntimeContext) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(proxyType);

        final Map<Method, MethodDescriptor> method2TaskTargetCache = createMethodCache(proxyType);

        ProxyInvocationHandler proxyInvocationHandler = new ProxyInvocationHandler(method2TaskTargetCache, injectedRuntimeContext);
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

        return (TargetInterface)enhancer.create();
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

    private Map<Method, MethodDescriptor> createMethodCache(Class target) {
        Map<Method, MethodDescriptor> method2TaskTargetCache = new HashMap<Method, MethodDescriptor>();

        Class<?> deciderInterface = AnnotationUtils.findAnnotatedClass(target, Decider.class);

        String deciderName = DeciderProxyFactory.deciderName(deciderInterface);
        String deciderVersion = DeciderProxyFactory.deciderVersion(deciderInterface);

        // find @Asynchronous methods
        Method[] targetMethods = target.getDeclaredMethods();
        for (Method method : targetMethods) {

            if (method.isAnnotationPresent(Asynchronous.class)) {

                if (!isPublicMethod(method)) {
                    throw new IncorrectAsynchronousMethodDefinition("Asynchronous method must be public", target);
                }

                TaskTarget taskTarget = new TaskTargetImpl(TaskType.DECIDER_ASYNCHRONOUS, deciderName, deciderVersion, method.getName());
/*              ToDo: ActorSchedulingOptions and Wait list is not supported for asynchronous decider methods
                Class<?>[] parameterTypes = method.getParameterTypes();
                int positionActorSchedulingOptions = positionParameter(parameterTypes, ActorSchedulingOptions.class);
                int positionPromisesWaitFor = positionOfWaitList(parameterTypes, positionActorSchedulingOptions);
*/
                AcceptFail acceptFail = method.getAnnotation(AcceptFail.class);
                MethodDescriptor descriptor = new MethodDescriptor(taskTarget, getArgTypes(method), -1, -1,
                        acceptFail != null, getFailNames(acceptFail));

                method2TaskTargetCache.put(method, descriptor);
            }

        }

        /**
         * Find @Execute method
         */
        for (Method method : deciderInterface.getDeclaredMethods()) {

            Execute executeAnnotation = method.getAnnotation(Execute.class);
            if (executeAnnotation != null) {

                if (!isPublicMethod(method)) {
                    throw new IncorrectExecuteMethodDefinition("@Execute method must be public", target);
                }

                String interfaceMethod = method.getName();

                for (Method implementationMethod : targetMethods) {

                    if (implementationMethod.getName().equals(interfaceMethod)) {
                        TaskTarget taskTarget = new TaskTargetImpl(TaskType.DECIDER_START, deciderName, deciderVersion, method.getName());
                        int positionActorSchedulingOptions = positionParameter(method.getParameterTypes(), TaskConfig.class);
                        int positionPromisesWaitFor = positionOfWaitList(method.getParameterTypes(), positionActorSchedulingOptions);
                        AcceptFail acceptFail = method.getAnnotation(AcceptFail.class);

                        MethodDescriptor descriptor = new MethodDescriptor(taskTarget, getArgTypes(method),
                                positionActorSchedulingOptions, positionPromisesWaitFor, acceptFail != null,
                                getFailNames(acceptFail));
                        method2TaskTargetCache.put(implementationMethod, descriptor);
                        break;
                    }
                }

                break;
            }

        }


        return method2TaskTargetCache;
    }

    private boolean isPublicMethod(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }
}

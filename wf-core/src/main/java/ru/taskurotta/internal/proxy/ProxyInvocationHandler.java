package ru.taskurotta.internal.proxy;

import ru.taskurotta.core.ArgType;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskOptions;
import ru.taskurotta.internal.core.TaskOptionsImpl;
import ru.taskurotta.exception.IllegalReturnTypeException;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.internal.core.MethodDescriptor;
import ru.taskurotta.internal.core.TaskImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * User: romario
 * Date: 1/14/13
 * Time: 5:44 PM
 */
public class ProxyInvocationHandler implements InvocationHandler {

    private Map<Method, MethodDescriptor> method2TaskTargetCache;

    private RuntimeContext injectedRuntimeProcess;

    public ProxyInvocationHandler(Map<Method, MethodDescriptor> method2TaskTargetCache, RuntimeContext injectedRuntimeProcess) {

        this.method2TaskTargetCache = method2TaskTargetCache;
        this.injectedRuntimeProcess = injectedRuntimeProcess;

    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        MethodDescriptor methodDescriptor = method2TaskTargetCache.get(method);

        ArgType[] argTypes = methodDescriptor.getArgTypes();
        TaskOptions taskOptions = null;

        if (argTypes != null) {
            taskOptions = new TaskOptionsImpl(argTypes);
        }

        RuntimeContext runtimeContext = null;

        if (injectedRuntimeProcess != null) {
            runtimeContext = injectedRuntimeProcess;
        } else {
            runtimeContext = RuntimeContext.getCurrent();
        }

        if (runtimeContext == null) {
            throw new IllegalAccessError("There is no RuntimeContext!");
        }

        UUID processId = runtimeContext.getProcessId();

        Task task = new TaskImpl(UUID.randomUUID(), processId, methodDescriptor.getTaskTarget(),
                System.currentTimeMillis(), 0,
                args, taskOptions);

        runtimeContext.handle(task);

        // First of all check return type
        Class<?> returnType = method.getReturnType();

        if (void.class.equals(returnType)) {
            return null;
        }

        if (Promise.class.isAssignableFrom(returnType)) {
            return Promise.createInstance(task.getId());
        } else {
            throw new IllegalReturnTypeException();
        }

    }
}

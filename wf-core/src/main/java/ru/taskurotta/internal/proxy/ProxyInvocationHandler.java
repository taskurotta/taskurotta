package ru.taskurotta.internal.proxy;

import ru.taskurotta.TaskHandler;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.exception.IllegalReturnTypeException;
import ru.taskurotta.internal.core.MethodDescriptor;
import ru.taskurotta.internal.core.TaskImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * User: romario
 * Date: 1/14/13
 * Time: 5:44 PM
 */
public class ProxyInvocationHandler implements InvocationHandler {

    private Map<Method, MethodDescriptor> method2TaskTargetCache;

    private TaskHandler taskHandler;

    public ProxyInvocationHandler(Map<Method, MethodDescriptor> method2TaskTargetCache, TaskHandler taskHandler) {

        this.method2TaskTargetCache = method2TaskTargetCache;
        this.taskHandler = taskHandler;

    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		MethodDescriptor methodDescriptor = method2TaskTargetCache.get(method);

        Task task = new TaskImpl(methodDescriptor.getTaskTarget(), args, methodDescriptor.getArgTypes());

        taskHandler.handle(task);

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

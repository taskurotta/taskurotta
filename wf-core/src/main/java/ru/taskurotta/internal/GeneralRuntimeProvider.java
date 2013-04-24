package ru.taskurotta.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Decider;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.ExponentialRetry;
import ru.taskurotta.annotation.LinearRetry;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.exception.IncorrectExecuteMethodDefinition;
import ru.taskurotta.exception.TaskTargetRequiredException;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.internal.proxy.DeciderProxyFactory;
import ru.taskurotta.policy.retry.ExponentialRetryPolicy;
import ru.taskurotta.policy.retry.LinearRetryPolicy;
import ru.taskurotta.policy.retry.RetryPolicy;
import ru.taskurotta.policy.retry.TimeRetryPolicyBase;
import ru.taskurotta.util.AnnotationUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * User: romario
 * Date: 1/22/13
 * Time: 4:33 PM
 */
public class GeneralRuntimeProvider implements RuntimeProvider {

    protected final static Logger log = LoggerFactory.getLogger(GeneralRuntimeProvider.class);

    protected static class TargetReference {
        private Object actorObject;
        private Method method;
        private RetryPolicy retryPolicy;

        private TargetReference(Object actorObject, Method method) {
            this.actorObject = actorObject;
            this.method = method;
        }

        private TargetReference(Object actorObject, Method method, RetryPolicy retryPolicy) {
            this.actorObject = actorObject;
            this.method = method;
            this.retryPolicy = retryPolicy;
        }

        public RetryPolicy getRetryPolicy() {
            return retryPolicy;
        }

        public Object invoke(Object... args) throws InvocationTargetException, IllegalAccessException {
            log.debug("before invoke {} args({})", method, args);
            return method.invoke(actorObject, args);
        }

        public String toString() {
            return actorObject.getClass().getName() + "." + method;
        }
    }


    public GeneralRuntimeProvider() {

    }


    @Override
    public RuntimeProcessor getRuntimeProcessor(Object actorBean) {

        Map<TaskTarget, TargetReference> taskTargetsMap;

        Class<?> workerInterface = AnnotationUtils.findAnnotatedClass(actorBean.getClass(), Worker.class);

        if (workerInterface != null) {
            taskTargetsMap = extractTargetsFromWorker(actorBean, workerInterface);
            return new GeneralRuntimeProcessor(taskTargetsMap);
        }


        Class<?> deciderInterface = AnnotationUtils.findAnnotatedClass(actorBean.getClass(), Decider.class);

        if (deciderInterface != null) {
            taskTargetsMap = extractTargetsFromDecider(actorBean, deciderInterface);
            return new GeneralRuntimeProcessor(taskTargetsMap);
        }

        throw new TaskTargetRequiredException(actorBean.getClass().getName());
    }


    /**
     * Method adds worker to registry
     *
     * @param workerBean      - workerBean object
     * @param workerInterface - interface of Worker workerBean
     */
    private Map<TaskTarget, TargetReference> extractTargetsFromWorker(Object workerBean, Class<?> workerInterface) {

        Map<TaskTarget, TargetReference> taskTargetsMap = null;

        Worker worker = workerInterface.getAnnotation(Worker.class);

        String workerName = worker.name();
        if (!StringUtils.hasText(workerName)) {
            workerName = workerInterface.getName();
        }

        String version = worker.version();

        Method[] targetMethods = workerInterface.getMethods();
        for (Method method : targetMethods) {
            TaskTarget key = createTaskTarget(TaskType.WORKER, workerName, version, method.getName());

            if (taskTargetsMap == null) {
                taskTargetsMap = new HashMap<TaskTarget, TargetReference>();
            }

            RetryPolicy retryPolicy = findAndCreateRetryPolicy(method);

            taskTargetsMap.put(key, new TargetReference(workerBean, method, retryPolicy));
        }

        if (taskTargetsMap == null) {
            throw new TaskTargetRequiredException(workerInterface.getName());
        }

        return taskTargetsMap;
    }


    /**
     * Method adds all decider methods to registry
     *
     * @param deciderBean      - deciderBean object
     * @param deciderInterface - interface of decider deciderBean
     */
    private Map<TaskTarget, TargetReference> extractTargetsFromDecider(Object deciderBean, Class<?> deciderInterface) {

        Map<TaskTarget, TargetReference> taskTargetsMap = null;

        String actorName = DeciderProxyFactory.deciderName(deciderInterface);
        String actorVersion = DeciderProxyFactory.deciderVersion(deciderInterface);

        // Find @Asynchronous methods
        for (Method method : deciderBean.getClass().getMethods()) {

            if (!method.isAnnotationPresent(Asynchronous.class)) {
                continue;
            }

            TaskTarget key = createTaskTarget(TaskType.DECIDER_ASYNCHRONOUS, actorName, actorVersion, method.getName());

            if (taskTargetsMap == null) {
                taskTargetsMap = new HashMap<TaskTarget, TargetReference>();
            }

            RetryPolicy retryPolicy = findAndCreateRetryPolicy(method);

            taskTargetsMap.put(key, new TargetReference(deciderBean, method, retryPolicy));
        }

        /**
         * Find @Execute method
         */
        boolean executeFound = false;
        for (Method method : deciderInterface.getMethods()) {

            if (!method.isAnnotationPresent(Execute.class)) {
                continue;
            }

            if (executeFound) {
                throw new IncorrectExecuteMethodDefinition("Decider interface has more then one @Execute annotation", deciderBean);
            }

            Class[] parameterTypes = method.getParameterTypes();
            for (Class type : parameterTypes) {
                //noinspection unchecked
                if (type.isAssignableFrom(Promise.class)) {
                    throw new IncorrectExecuteMethodDefinition("Parameters of @Execute method of decider are not allowed Promise", deciderBean);
                }
            }

            if (!method.getReturnType().isAssignableFrom(void.class) && !method.getReturnType().isAssignableFrom(Promise.class)) {
                throw new IncorrectExecuteMethodDefinition("@Execute method of decider should return 'void' or Promise", deciderBean);
            }

            TaskTarget key = createTaskTarget(TaskType.DECIDER_START, actorName, actorVersion, method.getName());

            if (taskTargetsMap == null) {
                taskTargetsMap = new HashMap<TaskTarget, TargetReference>();
            }

            RetryPolicy retryPolicy = findAndCreateRetryPolicy(method);

            taskTargetsMap.put(key, new TargetReference(deciderBean, method, retryPolicy));

            executeFound = true;

        }

        if (!executeFound) {
            throw new IncorrectExecuteMethodDefinition("Decider interface has no @Execute annotation", deciderBean);
        }

        if (taskTargetsMap == null) {
            throw new TaskTargetRequiredException(deciderInterface.getName());
        }

        return taskTargetsMap;
    }


    private TaskTarget createTaskTarget(TaskType taskType, String actorName, String version, String methodName) {
        return new TaskTargetImpl(taskType, actorName, version, methodName);
    }

    private RetryPolicy findAndCreateRetryPolicy(Method method) {
        TimeRetryPolicyBase retryPolicy = null;

        if (method.isAnnotationPresent(ExponentialRetry.class)) {
            ExponentialRetry annotationPolicy = method.getAnnotation(ExponentialRetry.class);
            retryPolicy = new ExponentialRetryPolicy(annotationPolicy.initialRetryIntervalSeconds());
            retryPolicy.setMaximumRetryIntervalSeconds(annotationPolicy.maximumRetryIntervalSeconds());
            retryPolicy.setMaximumAttempts(annotationPolicy.maximumAttempts());
            retryPolicy.setBackoffCoefficient(annotationPolicy.backoffCoefficient());
            retryPolicy.setRetryExpirationIntervalSeconds(annotationPolicy.retryExpirationSeconds());
        }

        if (method.isAnnotationPresent(LinearRetry.class)) {
            LinearRetry annotationPolicy = method.getAnnotation(LinearRetry.class);
            retryPolicy = new LinearRetryPolicy(annotationPolicy.initialRetryIntervalSeconds());
            retryPolicy.setMaximumRetryIntervalSeconds(annotationPolicy.maximumRetryIntervalSeconds());
            retryPolicy.setMaximumAttempts(annotationPolicy.maximumAttempts());
            retryPolicy.setRetryExpirationIntervalSeconds(annotationPolicy.retryExpirationSeconds());
        }

        return retryPolicy;
    }

}

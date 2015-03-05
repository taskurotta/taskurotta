package ru.taskurotta.internal.proxy;

import ru.taskurotta.annotation.AcceptFail;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.internal.core.MethodDescriptor;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * created by void 23.01.13 12:11
 */
public class ActorProxyFactory<ActorAnnotation extends Annotation, ClientAnnotation extends Annotation>
        extends CachedProxyFactory {

    private Class<ActorAnnotation> actorAnnotationClass;
    private Class<ClientAnnotation> clientAnnotationClass;
    private AnnotationExplorer annotationExplorer;

    public ActorProxyFactory(Class<ActorAnnotation> actorAnnotationClass,
                             Class<ClientAnnotation> clientAnnotationClass, AnnotationExplorer annotationExplorer) {
        this.actorAnnotationClass = actorAnnotationClass;
        this.clientAnnotationClass = clientAnnotationClass;
        this.annotationExplorer = annotationExplorer;
    }

    @Override
    public <TargetInterface> TargetInterface createProxy(Class<TargetInterface> proxyType, RuntimeContext injectedRuntimeContext) {
        Class clientInterface = ClientCheckerUtil.checkClientDefinition(proxyType, clientAnnotationClass);
        Class actorInterface = ClientCheckerUtil.checkActorDefinition(clientInterface, actorAnnotationClass,
                clientAnnotationClass, annotationExplorer);
        ClientCheckerUtil.checkInterfaceMatching(clientInterface, actorInterface);

        Map<Method, MethodDescriptor> method2TaskTargetCache = createMethodCache(clientInterface, actorInterface,
                actorAnnotationClass, annotationExplorer);
        ProxyInvocationHandler proxyInvocationHandler = new ProxyInvocationHandler(method2TaskTargetCache, injectedRuntimeContext);

        return createProxy(proxyType, proxyInvocationHandler);

    }


    /**
     * 2. Create cache of methods and TaskTarget objects
     *
     * @param clientInterface      - client interface. The source of methods
     * @param actorInterface       - actor interface. Source of the name and version
     * @param actorAnnotationClass - actor annotation
     * @param annotationExplorer   - actor annotation explorer
     * @return method to task target cache
     */
    private Map<Method, MethodDescriptor> createMethodCache(Class clientInterface, Class<?> actorInterface,
                                                            Class<? extends Annotation> actorAnnotationClass,
                                                            ProxyFactory.AnnotationExplorer annotationExplorer) {

        Annotation actorAnnotation = actorInterface.getAnnotation(actorAnnotationClass);

        String actorName = annotationExplorer.getActorName(actorAnnotation);
        if (StringUtils.isBlank(actorName)) {
            actorName = actorInterface.getName();
        }

        String actorVersion = annotationExplorer.getActorVersion(actorAnnotation);

        Map<Method, MethodDescriptor> method2TaskTargetCache = new HashMap<Method, MethodDescriptor>();

        Method[] targetMethods = clientInterface.getMethods();
        for (Method method : targetMethods) {
            TaskTarget taskTarget = new TaskTargetImpl(annotationExplorer.getTaskType(), actorName, actorVersion, method.getName());
            Class<?>[] parameterTypes = method.getParameterTypes();
            int positionActorSchedulingOptions = positionParameter(parameterTypes, TaskConfig.class);
            int positionPromisesWaitFor = positionOfWaitList(parameterTypes, positionActorSchedulingOptions);
            AcceptFail acceptFail = method.getAnnotation(AcceptFail.class);

            MethodDescriptor descriptor = new MethodDescriptor(taskTarget, getArgTypes(method),
                    positionActorSchedulingOptions, positionPromisesWaitFor, acceptFail != null, getFailNames(acceptFail));

            method2TaskTargetCache.put(method, descriptor);
        }
        return method2TaskTargetCache;
    }


    /**
     * Create proxy object for injection
     *
     * @param target                  - target class to be created
     * @param clientInvocationHandler - invocation handler for method calls
     * @return created proxy object
     */
    @SuppressWarnings("unchecked")
    public static <TargetInterface> TargetInterface createProxy(Class<TargetInterface> target,
                                                                ProxyInvocationHandler clientInvocationHandler) {

        return (TargetInterface) Proxy.newProxyInstance(target.getClassLoader(),
                new Class[]{target},
                clientInvocationHandler);
    }

}

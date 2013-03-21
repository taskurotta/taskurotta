package ru.taskurotta.internal.proxy;

import org.springframework.util.StringUtils;
import ru.taskurotta.TaskHandler;
import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.internal.core.TaskTargetImpl;

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
    public <TargetInterface> Object createProxy(Class<TargetInterface> proxyType, TaskHandler taskHandler) {
        Class clientInterface = ClientCheckerUtil.checkClientDefinition(proxyType, clientAnnotationClass);
        Class actorInterface = ClientCheckerUtil.checkActorDefinition(clientInterface, actorAnnotationClass,
                clientAnnotationClass, annotationExplorer);
        ClientCheckerUtil.checkInterfaceMatching(clientInterface, actorInterface);

        Map<Method, TaskTarget> method2TaskTargetCache = createMethodCache(clientInterface, actorInterface,
                actorAnnotationClass, annotationExplorer);
        ProxyInvocationHandler proxyInvocationHandler = new ProxyInvocationHandler(method2TaskTargetCache, taskHandler);

        return createProxy(proxyType, proxyInvocationHandler);

    }


    /**
     * 2. Create cache of methods and TaskTarget objects
     *
     * @param clientInterface      - client interface. The source of methods
     * @param actorInterface       - actor interface. Source of the name and version
     * @param actorAnnotationClass - actor annotation
     * @param annotationExplorer - actor annotation explorer
     * @return method to task target cache
     */
    public static Map<Method, TaskTarget> createMethodCache(Class clientInterface, Class<?> actorInterface,
                                                            Class<? extends Annotation> actorAnnotationClass,
                                                            ProxyFactory.AnnotationExplorer annotationExplorer) {

        Annotation actorAnnotation = actorInterface.getAnnotation(actorAnnotationClass);

        String actorName = annotationExplorer.getActorName(actorAnnotation);
        if (!StringUtils.hasText(actorName)) {
            actorName = actorInterface.getName();
        }

        String actorVersion = annotationExplorer.getActorVersion(actorAnnotation);

        Map<Method, TaskTarget> method2TaskTargetCache = new HashMap<Method, TaskTarget>();

        Method[] targetMethods = clientInterface.getMethods();
        for (Method method : targetMethods) {
            TaskTarget value = new TaskTargetImpl(annotationExplorer.getTaskType(), actorName, actorVersion, method.getName());
            method2TaskTargetCache.put(method, value);
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

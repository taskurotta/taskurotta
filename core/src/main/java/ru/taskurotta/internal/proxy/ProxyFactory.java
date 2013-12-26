package ru.taskurotta.internal.proxy;

import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.internal.core.TaskType;

import java.lang.annotation.Annotation;

/**
 * created by void 23.01.13 12:03
 */
public interface ProxyFactory {


    /**
     * Implementation of interface helps to get required attributes from annotations
     */
    public static interface AnnotationExplorer {
        /**
         * @param clientAnnotation -
         * @return annotation declared in client interface
         */
        Class getActorInterface(Annotation clientAnnotation);


        /**
         * @param actorAnnotation -
         * @return actor name from its annotation
         */
        String getActorName(Annotation actorAnnotation);


        /**
         * @param actorAnnotation -
         * @return actor version from its annotation
         */
        String getActorVersion(Annotation actorAnnotation);

        /**
         * @return type of task
         */
        TaskType getTaskType();

    }

    /**
     * Method performs compilation tasks for proxy of ***(Worker or Decider)Client. Compilation has following steps:
     * 1. Check ***Client interface and *** interface for required annotations
     * 2. Check ***Client methods and find appropriate *** methods
     * 3. Create cache of methods and TaskTarget objects
     * 4. Create InvocationHandler
     * 5. Create Proxy client
     *
     * @param targetInterface        - target type for proxy
     * @param injectedRuntimeContext - handler for method invocation of proxy object
     * @param <TargetInterface>      - interface definition
     * @return created proxy object
     */
    public <TargetInterface> TargetInterface create(Class<TargetInterface> targetInterface,
                                                    RuntimeContext injectedRuntimeContext);


}

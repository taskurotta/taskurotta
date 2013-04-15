package ru.taskurotta.internal.proxy;

import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.annotation.Execute;
import ru.taskurotta.annotation.NoWait;
import ru.taskurotta.core.ActorShedulingOptions;
import ru.taskurotta.core.Promise;
import ru.taskurotta.exception.ProxyFactoryException;
import ru.taskurotta.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * User: romario
 * Date: 1/23/13
 * Time: 4:10 PM
 */
public final class ClientCheckerUtil {

    /**
     * check if every method in client interface have corresponding method in actor interface.
     * Throws ProxyFactoryException if interfaces doesn't match
     *
     * @param clientInterface - client interface to check
     * @param actorInterface  - actor interface to compare to.
     */
    public static void checkInterfaceMatching(Class clientInterface, Class actorInterface) {
        // check: actor methods should have different names
        Map<String, Method> clientMethodMap = new HashMap<String, Method>();

        for (Method method : clientInterface.getDeclaredMethods()) {
            String methodName = method.getName();

            Method anotherMethodWithSameName = clientMethodMap.put(methodName, method);
            if (anotherMethodWithSameName != null) {
                throw new ProxyFactoryException("Method overloading are not supported. Actor interface has two methods ("
                        + methodName + ") with same name.");
            }
        }

        // check: all client methods should have matching
        for (Method actorMethod : actorInterface.getDeclaredMethods()) {
            Method clientMethod = clientMethodMap.get(actorMethod.getName());

            if (clientMethod == null) {
                throw new ProxyFactoryException("Actor (" + actorInterface.getName() + ") method ("
                        + actorMethod.getName() + ") has no match in client (" + clientInterface.getName() + ").");
            }

            checkMethodMatching(clientInterface, actorInterface, clientMethod, actorMethod);
        }
    }


    public static void checkMethodMatching(Class clientInterface, Class actorInterface, Method clientMethod, Method actorMethod) {
        // check: methods

        Class<?>[] actorParameterTypes = actorMethod.getParameterTypes();
        Class<?>[] clientParameterTypes = clientMethod.getParameterTypes();

        // check: list of method arguments should have equal length

        if (actorParameterTypes.length != clientParameterTypes.length) {

            int difference = clientParameterTypes.length - actorParameterTypes.length;
            Class lastArgumentClass;
            Class penultimateArgumentClass;

            switch (difference) {

                case 1:
                    lastArgumentClass = clientParameterTypes[clientParameterTypes.length - 1];

                    if (!lastArgumentClass.equals(Promise[].class)) {
                        throw new ProxyFactoryException("Last custom argument in client interface method " +
                                "must be Promise<?>..., but it: " + lastArgumentClass);
                    }

                    break;

                case 2:
                    lastArgumentClass = clientParameterTypes[clientParameterTypes.length - 1];
                    penultimateArgumentClass = clientParameterTypes[clientParameterTypes.length - 2];

                    if (!lastArgumentClass.equals(Promise[].class) && !penultimateArgumentClass.equals(ActorShedulingOptions.class)) {
                        throw new ProxyFactoryException("Last and penultimate custom arguments in client interface method must be in order " +
                                "(..., ActorShedulingOptions, Promise<?>...), but it: (..., " + penultimateArgumentClass + ", " + lastArgumentClass);
                    }

                    break;

                default:
                    throw new ProxyFactoryException("Quantity of client method parameters should be equals to actor method parameters: "
                            + "client (" + clientInterface.getName() + "), actor (" + actorInterface.getName() + "), method ("
                            + clientMethod.getName() + ").");
            }
        }

        // check: method arguments

        for (int i = 0; i < actorParameterTypes.length; i++) {

            // check: client method arguments should have matching
            // WARN: should we find the way to check generic parameter of the Promise class ?

            if (!clientParameterTypes[i].equals(actorParameterTypes[i])
                    && !clientParameterTypes[i].isAssignableFrom(Promise.class)) {

                throw new ProxyFactoryException("Client method parameters type should be either equal or Promise: "
                        + "client (" + clientInterface.getName() + "), actor (" + actorInterface.getName() + "), method ("
                        + clientMethod.getName() + ").");
            }


            if (actorParameterTypes[i].isAssignableFrom(Promise.class)) {
				Annotation[][] parameterAnnotations = actorMethod.getParameterAnnotations();
				boolean isNowait = false;
				for (int j=0; j< parameterAnnotations[i].length; j++) {
					if (parameterAnnotations[i][j] instanceof NoWait) {
						isNowait = true;
						break;
					}
				}
				if (!isNowait) {
					throw new ProxyFactoryException("Actor method parameters type shouldn't be Promise: "
							+ "client (" + clientInterface.getName() + "), Actor (" + actorInterface.getName() + "), method ("
							+ clientMethod.getName() + ").");
				}
            }

        }

        // check: method return type should have matching

        Class<?> actorReturnType = actorMethod.getReturnType();
        Class<?> clientReturnType = clientMethod.getReturnType();

        if (actorReturnType.isAssignableFrom(Promise.class)) {
            if (!actorMethod.isAnnotationPresent(Execute.class) && !actorMethod.isAnnotationPresent(Asynchronous.class)) {
                throw new ProxyFactoryException("Only @Execute or @Asynchronous can return Promise:"
                        + " actor (" + actorInterface.getName() + "), method (" + clientMethod.getName() + ").");
            }
        }

        if (!clientReturnType.isAssignableFrom(Promise.class)
                && !(clientReturnType.equals(Void.TYPE) && actorReturnType.equals(Void.TYPE))) {

            throw new ProxyFactoryException("Client method return type should be either Void or Promise"
                    + clientInterface.getName() + "), actor (" + actorInterface.getName()
                    + "), method (" + clientMethod.getName() + ").");
        }
    }


    /**
     * Check Actor Client interface for required annotations
     *
     * @param fieldType - Class of the field annotated for client injection
     * @return Class represents WorkerClient interface
     */
    public static Class checkClientDefinition(Class fieldType, Class<? extends Annotation> clientAnnotationClass) {

        /**
         * find WorkerClient annotation in class hierarchy of field object.
         */
        Class clientInterface = AnnotationUtils.findAnnotatedClass(fieldType, clientAnnotationClass);

        if (clientInterface == null || !clientInterface.isInterface()) {
            throw new ProxyFactoryException(
                    "Inject*Client annotation is supported only for fields witch interface is annotated as *Client: "
                            + fieldType);
        }
        return clientInterface;
    }

    /**
     * Check Actor interface for required annotations
     *
     * @param clientInterface - Client interface, the source of Actor interface
     * @return Class represents Actor interface
     */
    public static Class checkActorDefinition(Class clientInterface, Class<? extends Annotation> actorAnnotationClass,
                                             Class<? extends Annotation> clientAnnotationClass,
                                             ProxyFactory.AnnotationExplorer annotationExplorer) {
        /**
         * get Worker interface
         */
        @SuppressWarnings("unchecked")
        Annotation clientAnnotation = clientInterface.getAnnotation(clientAnnotationClass);

        Class actorInterface = annotationExplorer.getActorInterface(clientAnnotation);

        /**
         * check if AcctivitiesClient annotation holds default class.
         * check if it not specified on interface.
         */
        if (actorInterface.equals(Class.class) || !actorInterface.isInterface()) {
            throw new ProxyFactoryException(
                    "class of actor not specified in annotation for client interface: "
                            + clientInterface);
        }

        Class annotatedActorInterface = AnnotationUtils.findAnnotatedClass(actorInterface, actorAnnotationClass);

        /**
         * check if specified interface has Worker annotation
         */
        if (annotatedActorInterface == null) {
            throw new ProxyFactoryException(
                    "Specified actor interface has no required annotation^ " + actorInterface);
        }

        return annotatedActorInterface;
    }

}

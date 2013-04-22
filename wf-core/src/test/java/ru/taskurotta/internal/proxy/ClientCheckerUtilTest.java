package ru.taskurotta.internal.proxy;

import org.junit.Test;
import ru.taskurotta.annotation.Worker;
import ru.taskurotta.annotation.WorkerClient;
import ru.taskurotta.core.ActorSchedulingOptions;
import ru.taskurotta.core.Promise;
import ru.taskurotta.core.TaskType;
import ru.taskurotta.exception.ProxyFactoryException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * <p/>
 * User: romario
 * Date: 1/23/13
 * Time: 5:06 PM
 */
public class ClientCheckerUtilTest {

    @Worker
    public static interface BadClient {

        // check: list of method arguments should have equal length
        public void method1(String arg1, String arg2);

        public void method1(String arg1, String arg2, String arg3);

        // check: client method arguments should have matching
        public void method2(String arg1, int arg2);

        // check: client method arguments should have matching
        public void method3(String arg1, Promise<Integer> arg2, int arg3);

        public void method4(String arg1);

        // check: method return type should have matching
        public int method6(String arg1);

        public void method7(Promise<String> arg1);

        public void method4(String arg1, ActorSchedulingOptions actorSchedulingOptions, String arg2);

        public void method4(String arg1, ActorSchedulingOptions actorSchedulingOptions, String arg2, Promise<?> ... waitFor);
    }

    public static interface BadActor {

        public void method1(String arg1);

        public void method2(String arg1, double arg2);

        public void method3(String arg1, int arg2, double arg3);

        // check: method return type shouldn't be Promise
        public Promise<Void> method4(String arg1);

        public int method6(String arg1);

        // check: actor method arguments shouldn't be Promise
        public void method7(Promise<String> arg1);
    }

    @WorkerClient
    public interface IBad {
    }

    @Test(expected = ProxyFactoryException.class)
    public void testBadClientDefinition() {
        ClientCheckerUtil.checkClientDefinition(BadClient.class, WorkerClient.class);
    }

    @Test(expected = ProxyFactoryException.class)
    public void testBadActorDefinition() {
        ProxyFactory.AnnotationExplorer annotationExplorer = new ProxyFactory.AnnotationExplorer() {
            @Override
            public Class getActorInterface(Annotation clientAnnotation) {
                return ((WorkerClient) clientAnnotation).worker();
            }

            @Override
            public String getActorName(Annotation actorAnnotation) {
                return ((Worker) actorAnnotation).name();
            }

            @Override
            public String getActorVersion(Annotation actorAnnotation) {
                return ((Worker) actorAnnotation).version();
            }

            @Override
            public TaskType getTaskType() {
                return TaskType.WORKER;
            }
        };

        @Worker
        class Bad {
            public int max(int a, int b){
                return Math.max(a, b);
            }
        }

        @WorkerClient(worker = Bad.class)
        class BadClient {
            public Promise<Integer> max(int a, int b) {
                return Promise.asPromise(Math.max(a, b));
            }
        }

        ClientCheckerUtil.checkActorDefinition(BadClient.class, Worker.class, WorkerClient.class, annotationExplorer);

        ClientCheckerUtil.checkActorDefinition(IBad.class, Worker.class, WorkerClient.class, annotationExplorer);
    }

    @Test(expected = ProxyFactoryException.class)
    public void testBadInterfaceMatching() {
        Class clientInterface = BadClient.class;
        Class actorInterface = BadActor.class;

        ClientCheckerUtil.checkInterfaceMatching(clientInterface, actorInterface);
    }

    @Test
    public void testBadMethodMatching() {
        Class clientInterface = BadClient.class;
        Class actorInterface = BadActor.class;

        Map<String, Method> actorMethodMap = new HashMap<String, Method>();

        for (Method method : actorInterface.getDeclaredMethods()) {
            String methodName = method.getName();

            actorMethodMap.put(methodName, method);
        }


        // check: all client methods should have matching

        for (Method clientMethod : clientInterface.getDeclaredMethods()) {

            Method actorMethod = actorMethodMap.get(clientMethod.getName());

            try {
                ClientCheckerUtil.checkMethodMatching(clientInterface, actorInterface, clientMethod, actorMethod);
            } catch (ProxyFactoryException ex) {
                continue;
            }

            throw new RuntimeException("Check passed for wrong method. client (" + clientInterface + "), actor ("
                    + actorInterface + "), method (" + clientMethod + ")");
        }

    }

    @Worker
    public static interface IGoodClient {

        public void method1(String arg1, String arg2);

        public void method2(Promise<String> arg1, double arg2);

        public void method3(Promise<String> arg1, double arg2);

        public int method4(Promise<String> arg1, double arg2);
    }

    @WorkerClient(worker = IGoodClient.class)
    public static interface GoodClient {

        public void method1(String arg1, String arg2);

        public void method2(Promise<String> arg1, double arg2);

        public Promise<Void> method3(Promise<String> arg1, double arg2);

        public Promise<Integer> method4(Promise<String> arg1, double arg2);

        public void method1(String arg1, String arg2, ActorSchedulingOptions actorSchedulingOptions, Promise<?> ... waitFor);

        public void method1(String arg1, String arg2, ActorSchedulingOptions actorSchedulingOptions);

        public void method1(String arg1, String arg2, Promise<?> ... waitFor);
    }


    public static interface GoodActor {

        public void method1(String arg1, String arg2);

        public void method2(String arg1, double arg2);

        public void method3(String arg1, double arg2);

        public int method4(String arg1, double arg2);
    }

    @Test
    public void testGoodClientDefinition() {
        ClientCheckerUtil.checkClientDefinition(GoodClient.class, WorkerClient.class);
    }

    @Test
    public void testGoodActorDefinition() {
        ProxyFactory.AnnotationExplorer annotationExplorer = new ProxyFactory.AnnotationExplorer() {
            @Override
            public Class getActorInterface(Annotation clientAnnotation) {
                return ((WorkerClient) clientAnnotation).worker();
            }

            @Override
            public String getActorName(Annotation actorAnnotation) {
                return ((Worker) actorAnnotation).name();
            }

            @Override
            public String getActorVersion(Annotation actorAnnotation) {
                return ((Worker) actorAnnotation).version();
            }

            @Override
            public TaskType getTaskType() {
                return TaskType.WORKER;
            }
        };

        ClientCheckerUtil.checkActorDefinition(GoodClient.class, Worker.class, WorkerClient.class, annotationExplorer);
    }

    @Test
    public void testGoodInterfaceMatching() {
        Class clientInterface = GoodClient.class;
        Class actorInterface = GoodActor.class;

        ClientCheckerUtil.checkInterfaceMatching(clientInterface, actorInterface);
    }

    @Test
    public void testGoodMethodMatching() {
        Class clientInterface = GoodClient.class;
        Class actorInterface = GoodActor.class;

        Map<String, Method> actorMethodMap = new HashMap<String, Method>();

        for (Method method : actorInterface.getDeclaredMethods()) {
            String methodName = method.getName();

            actorMethodMap.put(methodName, method);
        }


        // check: all client methods should have matching

        for (Method clientMethod : clientInterface.getDeclaredMethods()) {

            Method actorMethod = actorMethodMap.get(clientMethod.getName());

            ClientCheckerUtil.checkMethodMatching(clientInterface, actorInterface, clientMethod, actorMethod);
        }

    }
}

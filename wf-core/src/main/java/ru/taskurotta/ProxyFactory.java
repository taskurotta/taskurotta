package ru.taskurotta;

import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.internal.proxy.AsynchronousDeciderProxyFactory;
import ru.taskurotta.internal.proxy.DeciderProxyFactory;
import ru.taskurotta.internal.proxy.WorkerProxyFactory;

/**
 * User: romario
 * Date: 3/26/13
 * Time: 12:56 AM
 */
public class ProxyFactory {

    private static WorkerProxyFactory workerProxyFactory = new WorkerProxyFactory();
    private static DeciderProxyFactory deciderProxyFactory = new DeciderProxyFactory();
    private static AsynchronousDeciderProxyFactory asynchronousDeciderProxyFactory = new AsynchronousDeciderProxyFactory();

    /**
     * get cached worker client proxy object
     *
     * @param type - interface of required object
     * @return proxy object or null
     */
    public static <WorkerClientType> WorkerClientType getWorkerClient(Class<WorkerClientType> type) {
        return workerProxyFactory.create(type, null);
    }


    /**
     * get cached decider client proxy object
     *
     * @param type - interface of required object
     * @return proxy object or null
     */
    public static <DeciderClientType> DeciderClientType getDeciderClient(Class<DeciderClientType> type) {
        return deciderProxyFactory.create(type, null);
    }


    /**
     * get cached decider client proxy object with specified TaskHandler
     *
     * @param type
     * @param injectedRuntimeContext
     * @param <DeciderClientType>
     * @return
     */
    public static <DeciderClientType> DeciderClientType getDeciderClient(Class<DeciderClientType> type, RuntimeContext injectedRuntimeContext) {
        return deciderProxyFactory.create(type, injectedRuntimeContext);
    }

    /**
     * get cached decider proxy object
     *
     * @param type          - interface of required object
     * @param <AsynchronousClientType> - class of actor bean
     * @return proxy object or null
     */
    public static <AsynchronousClientType> AsynchronousClientType getAsynchronousClient(Class<AsynchronousClientType> type) {
        return asynchronousDeciderProxyFactory.create(type, null);
    }

}

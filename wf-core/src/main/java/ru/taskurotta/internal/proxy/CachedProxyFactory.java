package ru.taskurotta.internal.proxy;

import ru.taskurotta.TaskHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * User: romario
 * Date: 2/5/13
 * Time: 3:37 PM
 */
abstract public class CachedProxyFactory implements ProxyFactory {


    private Map<Class, Object> clientToProxy = new HashMap<Class, Object>();

    abstract public <TargetInterface> Object createProxy(Class<TargetInterface> proxyType, TaskHandler taskHandler);

    @Override
    public <TargetInterface> TargetInterface create(Class<TargetInterface> targetInterface, TaskHandler taskHandler) {

        // should be not cached
        if (taskHandler != null) {
            return (TargetInterface) createProxy(targetInterface, taskHandler);
        }

        Object proxyClient = clientToProxy.get(targetInterface);

        if (proxyClient == null) {
            proxyClient = createProxy(targetInterface, null);

            clientToProxy.put(targetInterface, proxyClient);
        }

        return (TargetInterface) proxyClient;
    }

}

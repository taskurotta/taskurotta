package ru.taskurotta.hazelcast.lazymap;

import com.hazelcast.core.DistributedObject;

/**
 */
public class ProxyImpl implements DistributedObject, LazyMap {

    private String name;

    public ProxyImpl(String name) {
        this.name = name;
    }

    @Override
    public Object getId() {
        return name;
    }

    @Override
    public String getPartitionKey() {
        return "undefined";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getServiceName() {
        return SRV_NAME;
    }

    @Override
    public void destroy() {

    }
}

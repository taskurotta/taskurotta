package ru.taskurotta.hazelcast.lazymap;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.spi.RemoteService;

/**
 */
public class Service implements RemoteService {

    @Override
    public DistributedObject createDistributedObject(String objectName) {
        return new ProxyImpl(objectName);
    }

    @Override
    public void destroyDistributedObject(String objectName) {

    }
}

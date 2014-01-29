package ru.taskurotta.assemble;

import ru.taskurotta.hazelcast.queue.delay.Storage;
import ru.taskurotta.hazelcast.queue.delay.StorageFactory;

/**
 * Proxy for storage factory making spring config more flexible
 * Date: 28.01.14 15:34
 */
public class ProxyStorageFactory implements StorageFactory {

    private StorageFactory target;

    public ProxyStorageFactory(StorageFactory target) {
        this.target = target;
    }

    @Override
    public Storage createStorage(String queueName) {
        return target.createStorage(queueName);
    }
}

package ru.taskurotta.backend.hz.queue.delay;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:36 AM
 */
public interface StorageFactory {

    public Storage createStorage(String queueName);
}

package ru.taskurotta.hazelcast.delay;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:09 AM
 */
public interface QueueFactory {

    public DelayIQueue create(String queueName);
}

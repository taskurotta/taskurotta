package ru.taskurotta.backend.hz.queue.delay;

/**
 * User: romario
 * Date: 12/4/13
 * Time: 9:09 AM
 */
public interface QueueFactory {

    public DelayIQueue create(String queueName);
}

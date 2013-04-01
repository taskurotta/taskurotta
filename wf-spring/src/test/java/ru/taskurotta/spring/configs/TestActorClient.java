package ru.taskurotta.spring.configs;

import ru.taskurotta.annotation.WorkerClient;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 13:57
 */

@WorkerClient(worker = TestActor.class)
public interface TestActorClient {
    public int sum(int a, int b);
}

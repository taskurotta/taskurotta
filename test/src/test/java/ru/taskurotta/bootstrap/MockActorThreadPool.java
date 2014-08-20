package ru.taskurotta.bootstrap;

/**
 * User: dimadin
 * Date: 25.04.13
 * Time: 15:11
 */
public class MockActorThreadPool extends ActorThreadPool {

    public MockActorThreadPool(String actorClassName, int size) {
        super(actorClassName, null, size, 60000l);
    }

    @Override
    public synchronized boolean mute() {
        return false;
    }

    @Override
    public synchronized void wake() {
    }
}

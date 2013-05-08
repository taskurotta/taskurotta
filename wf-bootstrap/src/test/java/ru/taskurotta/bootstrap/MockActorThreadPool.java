package ru.taskurotta.bootstrap;

/**
 * User: dimadin
 * Date: 25.04.13
 * Time: 15:11
 */
public class MockActorThreadPool extends ActorThreadPool {

    public MockActorThreadPool(Class actorClass, int size) {
        super(actorClass, size);
    }

    @Override
    public synchronized boolean mute() {
        return false;
    }

    @Override
    public synchronized void wake() {
    }
}

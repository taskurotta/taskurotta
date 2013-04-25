package ru.taskurotta.bootstrap;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 25.04.13
 * Time: 15:11
 * To change this template use File | Settings | File Templates.
 */
public class MockActorThreadPool extends ActorThreadPool {

    public MockActorThreadPool(Class actorClass, int size) {
        super(actorClass, size);
    }

    @Override
    public synchronized boolean muteThreadPool() {
        return false;
    }

    @Override
    public synchronized void wakeThreadPool() {
        return;
    }
}

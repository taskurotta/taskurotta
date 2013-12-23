package ru.taskurotta.service.executor;

public interface Operation extends Runnable {

    public void init(Object nativePoint);

}

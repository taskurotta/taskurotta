package ru.taskurotta.bootstrap.pool;

import ru.taskurotta.bootstrap.ActorExecutor;

/**
 * Created on 27.08.2014.
 */
public interface ActorThreadPool {

    public void start(ActorExecutor actorExecutor);

    public boolean mute();

    public void wake();

    public void shutdown();

    public int getCurrentSize();

}

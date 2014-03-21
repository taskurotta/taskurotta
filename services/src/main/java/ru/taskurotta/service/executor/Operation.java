package ru.taskurotta.service.executor;

import java.io.Serializable;

public interface Operation extends Runnable, Serializable {

    public void init(Object nativePoint);

}

package ru.taskurotta.service.executor;

import java.io.Serializable;

public interface Operation<T> extends Runnable, Serializable {

    void init(T nativePoint);

}

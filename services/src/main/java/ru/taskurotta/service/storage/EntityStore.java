package ru.taskurotta.service.storage;

import java.util.Collection;

/**
 * Created on 08.06.2015.
 */
public interface EntityStore<E> {

    long add(E value);

    void remove(long id);

    void update(E entity, long id);

    Collection<Long> getKeys();

    E get(long id);

}

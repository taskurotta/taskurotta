package ru.taskurotta.util;

import java.util.concurrent.ConcurrentMap;

/**
 */
public class Concurrent {

    public static interface ValueFactory<T> {
        /**
         * Invoked to create new instance before trying putIfAbsent() call and after receiving null from get Map
         * operation.
         * @return new instance of Map value
         */
        public T newInstance();
    }

    public static interface ValueFactoryAndCallback<T> {
        /**
         * Invoked to create new instance before trying putIfAbsent() call and after receiving null from get Map
         * operation.
         * @return new instance of Map value
         */
        public T newInstance();

        /**
         * Invoked only if new instance inserted to the Map
         * @param object
         */
        public void callback(T object);
    }

    public static <K, V> V getOrCreate(ConcurrentMap<K, V> map, K key, ValueFactory<V> factory) {

        V value = map.get(key);
        if (value != null) {
            return value;
        }

        V newValue = factory.newInstance();
        value = map.putIfAbsent(key, newValue);

        if (value != null) {
            return value;
        }

        return newValue;
    }


    public static <K, V> V getOrCreate(ConcurrentMap<K, V> map, K key, ValueFactoryAndCallback<V> factory) {

        V value = map.get(key);
        if (value != null) {
            return value;
        }

        V newValue = factory.newInstance();
        value = map.putIfAbsent(key, newValue);

        if (value != null) {
            return value;
        }

        factory.callback(newValue);

        return newValue;
    }
}

/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.taskurotta.hazelcast.queue.store;

import java.util.Collection;
import java.util.Map;

/**
 * QueueStore makes a queue backed by a central data store; such as database, disk, etc.
 *
 * @param <T> queue item type
 */
public interface CachedQueueStore<T> {
    /**
     * Stores the key-value pair.
     *
     * @param key   key of the entry to store
     * @param value value of the entry to store
     */
    void store(Long key, T value);

    /**
     * Stores multiple entries. Implementation of this method can optimize the
     * store operation by storing all entries in one database connection for instance.
     *
     * @param map map of entries to store
     */
    void storeAll(Map<Long, T> map);

    /**
     * Deletes the entry with a given key from the store.
     *
     * @param key key to delete from the store.
     */
    void delete(Long key);

    /**
     * Deletes multiple entries from the store.
     *
     * @param keys keys of the entries to delete.
     */
    void deleteAll(Collection<Long> keys);

    void clear();

    /**
     * Loads the value of a given key. If distributed map does not contain the value
     * for the given key, then Hazelcast will call the implementation load (key) method
     * to obtain the value. Implementation can use any means of loading the given key;
     * such as an O/R mapping tool, simple SQL, reading a file, etc.
     *
     * @param key
     * @return value of the key
     */
    T load(Long key);


    /**
     * Loads all entries from specified interval. This is a batch load operation so that implementation can
     * optimize the multiple loads.
     *
     * @param from started id of interval (included)
     * @param to finished id interval (included)
     * @return map of loaded key-value pairs.
     */

    Map<Long, T> loadAll(long from, long to);


    /**
     * @return min Id of the stored items
     */
    public long getMinItemId();

    /**
     *
     * @return max Id of the stored items
     */
    public long getMaxItemId();
}

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

package ru.taskurotta.hazelcast.queue.impl;

import com.hazelcast.nio.serialization.ArrayDataSerializableFactory;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.util.ConstructorFunction;
import ru.taskurotta.hazelcast.queue.impl.operations.AddAllOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.CheckAndEvictOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.ClearOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.CompareAndRemoveOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.ContainsOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.DrainOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.IsEmptyOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.IteratorOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.OfferOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.PeekOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.PollOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.QueueReplicationOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.RemainingCapacityOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.RemoveOperation;
import ru.taskurotta.hazelcast.queue.impl.operations.SizeOperation;

/**
 * A {@link com.hazelcast.nio.serialization.DataSerializerHook} for the queue operations and support structures.
 */
public final class QueueDataSerializerHook {

    public static final int F_ID = 1;

    public static final int OFFER = 0;
    public static final int POLL = 1;
    public static final int PEEK = 2;

    public static final int ADD_ALL = 3;
    public static final int CLEAR = 4;
    public static final int COMPARE_AND_REMOVE = 5;
    public static final int CONTAINS = 6;
    public static final int DRAIN = 7;
    public static final int ITERATOR = 8;
    public static final int QUEUE_EVENT = 9;
    public static final int QUEUE_EVENT_FILTER = 10;
    public static final int QUEUE_ITEM = 11;
    public static final int QUEUE_REPLICATION = 12;
    public static final int REMOVE = 13;
    public static final int SIZE = 14;
    public static final int CHECK_EVICT = 15;
    public static final int QUEUE_CONTAINER = 16;
    public static final int IS_EMPTY = 17;
    public static final int REMAINING_CAPACITY = 18;


    public static DataSerializableFactory createFactory() {

        ConstructorFunction<Integer, IdentifiedDataSerializable>[] constructors = new ConstructorFunction[REMAINING_CAPACITY + 1];
        constructors[OFFER] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new OfferOperation();
            }
        };

        constructors[POLL] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new PollOperation();
            }
        };
        constructors[PEEK] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new PeekOperation();
            }
        };
        constructors[ADD_ALL] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new AddAllOperation();
            }
        };
        constructors[CLEAR] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new ClearOperation();
            }
        };
        constructors[COMPARE_AND_REMOVE] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new CompareAndRemoveOperation();
            }
        };
        constructors[CONTAINS] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new ContainsOperation();
            }
        };
        constructors[DRAIN] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new DrainOperation();
            }
        };
        constructors[ITERATOR] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new IteratorOperation();
            }
        };
        constructors[QUEUE_EVENT] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new QueueEvent();
            }
        };
        constructors[QUEUE_EVENT_FILTER] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new QueueEventFilter();
            }
        };
        constructors[QUEUE_ITEM] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new QueueItem(null);
            }
        };
        constructors[QUEUE_REPLICATION] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new QueueReplicationOperation();
            }
        };
        constructors[REMOVE] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new RemoveOperation();
            }
        };
        constructors[SIZE] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new SizeOperation();
            }
        };
        constructors[CHECK_EVICT] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new CheckAndEvictOperation();
            }
        };
        constructors[QUEUE_CONTAINER] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new QueueContainer(null);
            }
        };
        constructors[IS_EMPTY] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            @Override
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new IsEmptyOperation();
            }
        };
        constructors[REMAINING_CAPACITY] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            @Override
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new RemainingCapacityOperation();
            }
        };

        return new ArrayDataSerializableFactory(constructors);
    }
}

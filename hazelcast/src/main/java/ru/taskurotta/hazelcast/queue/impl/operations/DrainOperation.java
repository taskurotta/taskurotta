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

package ru.taskurotta.hazelcast.queue.impl.operations;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.Notifier;
import com.hazelcast.spi.WaitNotifyKey;
import com.hazelcast.spi.impl.SerializableCollection;
import ru.taskurotta.hazelcast.queue.impl.QueueContainer;
import ru.taskurotta.hazelcast.queue.impl.QueueDataSerializerHook;
import ru.taskurotta.hazelcast.queue.impl.stats.LocalCachedQueueStatsImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class drain items according to drain condition.
 */
public class DrainOperation extends QueueOperation implements Notifier {

    private int maxSize;
    private Map<Long, Data> dataMap;

    public DrainOperation() {
    }

    public DrainOperation(String name, int maxSize) {
        super(name);
        this.maxSize = maxSize;
    }

    @Override
    public void run() throws Exception {
        QueueContainer container = getOrCreateContainer();
        dataMap = container.drain(maxSize);
        response = new SerializableCollection(new ArrayList<Data>(dataMap.values()));
    }

    @Override
    public void afterRun() throws Exception {
        LocalCachedQueueStatsImpl stats = getQueueService().getLocalQueueStatsImpl(name);
        stats.incrementOtherOperations();
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        out.writeInt(maxSize);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        maxSize = in.readInt();
    }

    @Override
    public boolean shouldNotify() {
        return dataMap.size() > 0;
    }

    @Override
    public WaitNotifyKey getNotifiedKey() {
        return getOrCreateContainer().getOfferWaitNotifyKey();
    }

    @Override
    public int getId() {
        return QueueDataSerializerHook.DRAIN;
    }
}

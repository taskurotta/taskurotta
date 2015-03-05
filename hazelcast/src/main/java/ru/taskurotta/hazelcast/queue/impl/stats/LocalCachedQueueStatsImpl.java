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

package ru.taskurotta.hazelcast.queue.impl.stats;

import com.hazelcast.com.eclipsesource.json.JsonObject;
import com.hazelcast.util.Clock;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static com.hazelcast.util.JsonUtil.getLong;

public class LocalCachedQueueStatsImpl implements LocalCachedQueueStats {

    private static final AtomicLongFieldUpdater<LocalCachedQueueStatsImpl> NUMBER_OF_OFFERS_UPDATER = AtomicLongFieldUpdater
            .newUpdater(LocalCachedQueueStatsImpl.class, "numberOfOffers");
    private static final AtomicLongFieldUpdater<LocalCachedQueueStatsImpl> NUMBER_OF_REJECTED_OFFERS_UPDATER = AtomicLongFieldUpdater
            .newUpdater(LocalCachedQueueStatsImpl.class, "numberOfRejectedOffers");
    private static final AtomicLongFieldUpdater<LocalCachedQueueStatsImpl> NUMBER_OF_POLLS_UPDATER = AtomicLongFieldUpdater
            .newUpdater(LocalCachedQueueStatsImpl.class, "numberOfPolls");
    private static final AtomicLongFieldUpdater<LocalCachedQueueStatsImpl> NUMBER_OF_EMPTY_POLLS_UPDATER = AtomicLongFieldUpdater
            .newUpdater(LocalCachedQueueStatsImpl.class, "numberOfEmptyPolls");
    private static final AtomicLongFieldUpdater<LocalCachedQueueStatsImpl> NUMBER_OF_OTHER_OPERATIONS_UPDATER = AtomicLongFieldUpdater
            .newUpdater(LocalCachedQueueStatsImpl.class, "numberOfOtherOperations");
    private static final AtomicLongFieldUpdater<LocalCachedQueueStatsImpl> NUMBER_OF_EVENTS_UPDATER = AtomicLongFieldUpdater
            .newUpdater(LocalCachedQueueStatsImpl.class, "numberOfEvents");

    private long creationTime;

    private long heapCost;
    private long cacheSize;
    private long cacheMaxSize;

    // These fields are only accessed through the updater
    private volatile long numberOfOffers;
    private volatile long numberOfRejectedOffers;
    private volatile long numberOfPolls;
    private volatile long numberOfEmptyPolls;
    private volatile long numberOfOtherOperations;
    private volatile long numberOfEvents;

    public LocalCachedQueueStatsImpl() {
        creationTime = Clock.currentTimeMillis();
    }

    @Override
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.add("creationTime", creationTime);
        root.add("heapCost", heapCost);
        root.add("cacheSize", cacheSize);
        root.add("cacheMaxSize", cacheMaxSize);
        root.add("numberOfOffers", numberOfOffers);
        root.add("numberOfPolls", numberOfPolls);
        root.add("numberOfRejectedOffers", numberOfRejectedOffers);
        root.add("numberOfEmptyPolls", numberOfEmptyPolls);
        root.add("numberOfOtherOperations", numberOfOtherOperations);
        root.add("numberOfEvents", numberOfEvents);
        return root;
    }

    @Override
    public void fromJson(JsonObject json) {
        creationTime = getLong(json, "creationTime", -1L);
        heapCost = getLong(json, "heapCost", -1L);
        cacheSize = getLong(json, "cacheSize", -1L);
        cacheMaxSize = getLong(json, "cacheMaxSize", -1L);
        NUMBER_OF_OFFERS_UPDATER.set(this, getLong(json, "numberOfOffers", -1L));
        NUMBER_OF_POLLS_UPDATER.set(this, getLong(json, "numberOfPolls", -1L));
        NUMBER_OF_REJECTED_OFFERS_UPDATER.set(this, getLong(json, "numberOfRejectedOffers", -1L));
        NUMBER_OF_EMPTY_POLLS_UPDATER.set(this, getLong(json, "numberOfEmptyPolls", -1L));
        NUMBER_OF_OTHER_OPERATIONS_UPDATER.set(this, getLong(json, "numberOfOtherOperations", -1L));
        NUMBER_OF_EVENTS_UPDATER.set(this, getLong(json, "numberOfEvents", -1L));
    }

    @Override
    public long getHeapCost() {
        return heapCost;
    }

    @Override
    public long getCacheSize() {
        return cacheSize;
    }

    @Override
    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setHeapCost(long heapCost) {
        this.heapCost = heapCost;
    }

    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    public long total() {
        return numberOfOffers + numberOfPolls + numberOfOtherOperations;
    }

    @Override
    public long getOfferOperationCount() {
        return numberOfOffers;
    }

    @Override
    public long getRejectedOfferOperationCount() {
        return numberOfRejectedOffers;
    }

    @Override
    public long getPollOperationCount() {
        return numberOfPolls;
    }

    @Override
    public long getEmptyPollOperationCount() {
        return numberOfEmptyPolls;
    }

    @Override
    public long getOtherOperationsCount() {
        return numberOfOtherOperations;
    }

    public void incrementOtherOperations() {
        NUMBER_OF_OTHER_OPERATIONS_UPDATER.incrementAndGet(this);
    }

    public void incrementOffers() {
        NUMBER_OF_OFFERS_UPDATER.incrementAndGet(this);
    }

    public void incrementRejectedOffers() {
        NUMBER_OF_REJECTED_OFFERS_UPDATER.incrementAndGet(this);
    }

    public void incrementPolls() {
        NUMBER_OF_POLLS_UPDATER.incrementAndGet(this);
    }

    public void incrementEmptyPolls() {
        NUMBER_OF_EMPTY_POLLS_UPDATER.incrementAndGet(this);
    }

    public void incrementReceivedEvents() {
        NUMBER_OF_EVENTS_UPDATER.incrementAndGet(this);
    }

    @Override
    public long getEventOperationCount() {
        return numberOfEvents;
    }
}

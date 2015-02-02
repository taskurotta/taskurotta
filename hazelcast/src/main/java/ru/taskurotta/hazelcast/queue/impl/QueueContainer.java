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

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.nio.serialization.SerializationService;
import com.hazelcast.spi.NodeEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class contains methods be notable for the Queue.
 * such as pool,peek,clear..
 */
public class QueueContainer implements IdentifiedDataSerializable {

    private static final Logger logger = LoggerFactory.getLogger(QueueContainer.class);

    private static final boolean DEBUG_FULL = false;
    private static final boolean DEBUG_LOAD = false;
    private static final boolean DEBUG_RESIZE = true;

    private CachedQueueConfig config;
    private QueueStoreWrapper store;
    private NodeEngine nodeEngine;
    private QueueService service;

    private final QueueWaitNotifyKey pollWaitNotifyKey;
    private final QueueWaitNotifyKey offerWaitNotifyKey;

    private String name;
    // new stuff

    protected long headId = 0;
    protected long tailId = -1;
    protected boolean bufferClosed = false;

    protected Map<Long, QueueItem> buffer = new HashMap<>();

    protected int maxBufferSize;
    protected long heapCost = 0;

    private boolean isEvictionScheduled = false;


    public int size() {
        return (int) (tailId - headId + 1);
    }

    private boolean isEmpty() {
        return headId > tailId;
    }

    public long offer(Data data) {

        if (DEBUG_FULL) logger.debug("offer(): name = {} buffer.size() = {} headId = {} tailId = {} " +
                "bufferClosed = {}", name, buffer.size(), headId, tailId, bufferClosed);

        final long itemId = nextId();

        if (DEBUG_FULL) logger.debug("offer(): name = {} next item id = {}", name, itemId);

        store.store(itemId, data);

        if (!bufferClosed) {
            int bufferSize = buffer.size();
            if (maxBufferSize > bufferSize) {

                if (DEBUG_FULL) logger.debug("offer(): name = {} add item to buffer. itemId = {}", name, itemId);

                QueueItem item = new QueueItem(data);
                addItemToBuffer(itemId, item);

                if (maxBufferSize == bufferSize + 1) {

                    if (DEBUG_FULL) logger.debug("offer(): name = {} close buffer. buffer.size() = {}",
                            name, buffer.size());

                    bufferClosed = true;
                }
            }
        }

        cancelEvictionIfExists();
        return itemId;
    }

    private void addItemToBuffer(long key, QueueItem item) {

        heapCost += item.headCost;

        buffer.put(key, item);
    }

    private QueueItem removeItemFromBuffer(long key) {

        final QueueItem item = buffer.remove(key);

        if (item != null) {
            heapCost -= item.headCost;
        }
        return item;
    }

    public QueueItem poll() {
        if (DEBUG_FULL) logger.debug("poll(): name = {} buffer.size() = {} headId = {} tailId = {} " +
                "bufferClosed = {}", name, buffer.size(), headId, tailId, bufferClosed);

        while(true) {
            if (isEmpty()) {
                bufferClosed = false;
                scheduleEvictionIfEmpty();
                return null;
            }

            if (buffer.isEmpty()) {
                loadBuffer();
                // open buffer if it is not full
                if (size() <= maxBufferSize) {
                    bufferClosed = false;
                } else {
                    bufferClosed = true;
                }
            }

            long currHeadId = shiftHead();
            if (DEBUG_FULL) logger.debug("poll(): name = {}, currHeadId = {}", name, currHeadId);
            store.delete(currHeadId);
            QueueItem item = removeItemFromBuffer(currHeadId);

            if (item == null) {
                logger.warn("poll(): name = {}. Entry {} not found in cache of queue container", name, currHeadId);
                while (buffer.size() != 0 && item == null) {
                    currHeadId = shiftHead();
                    store.delete(currHeadId);
                    item = removeItemFromBuffer(currHeadId);
                }
                if (item == null) {
                    reset();
                } else {
                    scheduleEvictionIfEmpty();
                    return item;
                }
            } else {
                if (DEBUG_FULL) logger.debug("poll(): name = {}", name);
                scheduleEvictionIfEmpty();
                return item;
            }
        }
    }

    private void reset() {
        tailId = store.getMaxItemId();
        headId = store.getMinItemId();
        buffer.clear();
        heapCost = 0;
    }


    public void loadBuffer() {

        long maxId = headId + maxBufferSize - 1;

        if (maxId >= tailId) {
            maxId = tailId;
        }

        loadAll(headId, maxId);
    }

    private void loadAll(long from, long to) {

        if (DEBUG_FULL || DEBUG_LOAD) logger.debug("loadAll(): name = {} before load: buffer size = {} from = {} to =" +
                " {}", name, buffer.size(), from, to);


        for (Map.Entry<Long, Data> entry : store.loadAll(from, to).entrySet()) {
            Long itemId = entry.getKey();
            QueueItem item = new QueueItem(entry.getValue());

            addItemToBuffer(itemId, item);
        }

        if (DEBUG_FULL || DEBUG_LOAD) logger.debug("loadAll(): name = {}, after load: buffer size = {} from = {} to =" +
                " {}", name, buffer.size(), from, to);
    }

    public long getHeapCost() {
        return heapCost;
    }

    public long getCacheSize() {
        return buffer.size();
    }


    public long getCacheMaxSize() {
        return maxBufferSize;
    }

    /**
     * initialization of head and tail id.
     * load date to the buffer
     */
    public void init() {

        maxBufferSize = config.getCacheSize();

        tailId = store.getMaxItemId();
        headId = store.getMinItemId();

        loadBuffer();

        if (size() >= maxBufferSize) {
            bufferClosed = true;
        } else {
            bufferClosed = false;
        }
        // todo: may be we should close buffer after loading?
        //resizeBuffer();

        scheduleEvictionIfEmpty();
    }

    long nextId() {
        return ++tailId;
    }

    long shiftHead() {
        return headId++;
    }


//    private void scheduleEvictionIfEmpty() {
//
//        service.scheduleEviction(name, 5000);
//    }

    private void scheduleEvictionIfEmpty() {
        final int emptyQueueTtl = config.getEmptyQueueTtl();
        if (emptyQueueTtl < 0) {
            return;
        }
        if (isEmpty() && !isEvictionScheduled) {
            if (emptyQueueTtl == 0) {
                nodeEngine.getProxyService().destroyDistributedObject(CachedQueue.class.getName(), name);
            } else if (emptyQueueTtl > 0) {
                logger.debug("Scheduling eviction emptyQueueTtl = {} seconds", emptyQueueTtl);
                service.scheduleEviction(name, TimeUnit.SECONDS.toMillis(emptyQueueTtl));
                isEvictionScheduled = true;
            }
        }
    }

    public void cancelEvictionIfExists() {
        if (isEvictionScheduled) {
            service.cancelEviction(name);
            logger.debug("Scheduling eviction canceled");
            isEvictionScheduled = false;
        }
    }


    public boolean isEvictable() {
        service.scheduleEviction(name, 5000);

        //resizeBuffer();
        return false;
    }

    protected void resizeBuffer(int newSize) {

        int buffSize = buffer.size();

        if (DEBUG_FULL || DEBUG_RESIZE) logger.debug("resizeBuffer(): name = {}, oldSize = {} newSize = {} bufferSize" +
                        " = {} isEmpty = {} bufferClosed = {}, size() = {}, headId = {}, tailId = {}", name, maxBufferSize,
                newSize, buffSize, isEmpty(), bufferClosed, size(), headId, tailId);

        if (buffSize > maxBufferSize) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (maxBufferSize == newSize) {
            if (DEBUG_FULL || DEBUG_RESIZE) logger.debug("resizeBuffer(): name = {} nothing to do", name);
            return;
        }

        if (!isEmpty()) {

            if (!bufferClosed) {

                if (newSize < buffSize) {

                    if (DEBUG_FULL || DEBUG_RESIZE)
                        logger.debug("resizeBuffer(): name = {} drain open buffer and close it", name);

                    long to = tailId;
                    long from = to - (buffSize - newSize) + 1;

                    removeFromBuffer(from, to);

                    bufferClosed = true;
                } else {

                    if (buffer.size() == newSize) {
                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} close opened buffer", name);
                        bufferClosed = true;
                    } else {
                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} nothing to do at this moment", name);
                    }
                }

            } else {

                if (newSize < buffSize) {

                    if (DEBUG_FULL || DEBUG_RESIZE) logger.debug("resizeBuffer(): name = {} drain closed buffer", name);

                    long to = headId + buffSize - 1;
                    long from = headId + newSize;

                    removeFromBuffer(from, to);
                } else {

                    int needMaximumToLoad = newSize - buffSize;

                    if (tailId > headId + needMaximumToLoad) {
                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} need load {} and NOT open buffer. " +
                                    "queue size = {} buffer.size() = {}", name, needMaximumToLoad, size(), buffer.size());

                        loadAll(headId + maxBufferSize, headId + maxBufferSize + needMaximumToLoad - 1);

                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} after load buffer.size() = {}",
                                    name, buffer.size());
                    } else {
                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} need load {} and OPEN buffer. " +
                                    "queue size = {} buffer.size() = {}", name, tailId - headId + 1, size(), buffer.size());

                        loadAll(headId + maxBufferSize, tailId);
                        bufferClosed = false;

                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} after load buffer.size() = {}",
                                    name, buffer.size());
                    }
                }
            }
        } else {
            if (DEBUG_FULL || DEBUG_RESIZE)
                logger.debug("resizeBuffer(): name = {} nothing to do with empty queue", name);
        }


        maxBufferSize = newSize;
    }

    private void removeFromBuffer(long from, long to) {

        if (DEBUG_FULL || DEBUG_RESIZE) logger.debug("removeFromBuffer(): name = {} from = {} to = {} quantity  = ",
                name, from, to, to - from + 1);

        int j = 0;

        for (long i = from; i <= to; i++) {
            if (removeItemFromBuffer(i) != null) {
                j++;
            }
        }

        if (DEBUG_FULL || DEBUG_RESIZE) logger.debug("removeFromBuffer(): name = {} removeFromBuffer() removed = " +
                "{} buffer.size() = {}", j, buffer.size());
    }


    // old stuff

    public QueueContainer(String name) {
        this.name = name;

        pollWaitNotifyKey = new QueueWaitNotifyKey(name, "poll");
        offerWaitNotifyKey = new QueueWaitNotifyKey(name, "offer");
    }

    QueueContainer(String name, CachedQueueConfig config, NodeEngine nodeEngine, QueueService service) throws
            Exception {
        this(name);
        setConfig(config, nodeEngine, service);
    }


    public Map<Long, Data> addAll(Collection<Data> dataList) {
        throw new IllegalStateException("Not implemented yet!");
    }

    public QueueItem peek() {
        throw new IllegalStateException("Not implemented yet!");
    }

    // todo: not needed. should be removed
    public Map<Long, Data> drain(int maxSize) {
        throw new IllegalStateException("Not implemented yet!");
    }

    public void clear() {
        store.clear();
        headId = 0;
        tailId = -1;
        heapCost = 0;
        buffer.clear();
        bufferClosed = false;
        scheduleEvictionIfEmpty();
    }

    // todo: not needed. should be removed
    public long remove(Data data) {
        throw new IllegalStateException("Not implemented yet!");
    }

    // todo: not needed. should be removed
    public boolean contains(Collection<Data> dataSet) {
        throw new IllegalStateException("Not implemented yet!");
    }

    // todo: not needed. should be removed
    public List<Data> getAsDataList() {
        throw new IllegalStateException("Not implemented yet!");
    }

    // todo: not needed. should be removed
    public Map<Long, Data> compareAndRemove(Collection<Data> dataList, boolean retain) {
        throw new IllegalStateException("Not implemented yet!");
    }

    public void setConfig(CachedQueueConfig config, NodeEngine nodeEngine, QueueService service) {
        this.nodeEngine = nodeEngine;
        this.service = service;
        this.config = new CachedQueueConfig(config);

        // init queue store.
        final CachedQueueStoreConfig storeConfig = config.getQueueStoreConfig();
        final SerializationService serializationService = nodeEngine.getSerializationService();
        this.store = QueueStoreWrapper.create(name, storeConfig, serializationService);

    }


    public QueueWaitNotifyKey getPollWaitNotifyKey() {
        return pollWaitNotifyKey;
    }

    public QueueWaitNotifyKey getOfferWaitNotifyKey() {
        return offerWaitNotifyKey;
    }

    public CachedQueueConfig getConfig() {
        return config;
    }

    // todo: not needed. should be removed
    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
    }

    public void destroy() {
    }

    @Override
    public int getFactoryId() {
        return QueueDataSerializerHook.F_ID;
    }

    @Override
    public int getId() {
        return QueueDataSerializerHook.QUEUE_CONTAINER;
    }
}

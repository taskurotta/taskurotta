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

import com.hazelcast.internal.serialization.SerializationService;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.NodeEngine;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class contains methods be notable for the Queue.
 * such as pool,peek,clear..
 */
public class QueueContainer {

    private static final Logger logger = LoggerFactory.getLogger(QueueContainer.class);

    public static AtomicInteger addedTaskToQueue = new AtomicInteger();

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

    private Meter pollNotNullMeter;
    private Timer offerTimer;
    private SizeAdviser sizeAdviser;

    QueueContainer(String queueName, CachedQueueConfig config, NodeEngine nodeEngine, QueueService service) throws
            Exception {

        this.name = queueName;

        pollWaitNotifyKey = new QueueWaitNotifyKey(name, "poll");
        offerWaitNotifyKey = new QueueWaitNotifyKey(name, "offer");

        setConfig(config, nodeEngine, service);

        final String hzInstanceName = nodeEngine.getHazelcastInstance().getName();

        this.pollNotNullMeter = SizeAdviser.getPollNotNullMeter(hzInstanceName, queueName);
        this.offerTimer = SizeAdviser.getOfferTimer(hzInstanceName, queueName);
        this.sizeAdviser = service.getSizeAdviser();
    }


    public int size() {
        return (int) (tailId - headId + 1);
    }

    private boolean isEmpty() {
        return headId > tailId;
    }

    // todo: why it returns data?
    public long offer(Data data) {

        if (DEBUG_FULL) logger.debug("offer(): name = {}#{} buffer.size() = {} headId = {} tailId = {} " +
                "bufferClosed = {}", name, this.hashCode(), buffer.size(), headId, tailId, bufferClosed);

        offerTimer.update(data.getHeapCost(), TimeUnit.NANOSECONDS);

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

        addedTaskToQueue.incrementAndGet();
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
        if (DEBUG_FULL) logger.debug("poll(): name = {}#{} buffer.size() = {} headId = {} tailId = {} " +
                "bufferClosed = {}", name, this.hashCode(), buffer.size(), headId, tailId, bufferClosed);

        while (true) {
            if (isEmpty()) {
                bufferClosed = false;
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
                logger.warn("poll(): name = {}. Entry {} not found in cache of queue container. buffer.size() = {}, " +
                                "bufferClosed = {}, size() = {}, maxBufferSize = {}, headId = {}, tailId = {}",
                        name, currHeadId, buffer.size(), bufferClosed, size(), maxBufferSize, headId, tailId);
                while (buffer.size() != 0 && item == null) {
                    currHeadId = shiftHead();
                    store.delete(currHeadId);
                    item = removeItemFromBuffer(currHeadId);
                }
                if (item == null) {
                    reset();
                } else {
                    if (DEBUG_FULL) logger.debug("poll(): name = {}", name);

                    pollNotNullMeter.mark();
                    return item;
                }
            } else {
                if (DEBUG_FULL) logger.debug("poll(): name = {}", name);

                pollNotNullMeter.mark();
                return item;
            }
        }
    }

    private void reset() {
        if (store.isEnabled()) {
            tailId = store.getMaxItemId();
            headId = store.getMinItemId();
            buffer.clear();
            heapCost = 0;
        }
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

        Map<Long, Data> loadedData = store.loadAll(from, to);
        if (loadedData.size() != to - from + 1) {
            logger.warn("loadAll(): name = {} Store returns less than expected quantity of items. expected = {}, " +
                    "actual = {}, from = {}, to = {}, headId = {}, tailId = {} ", name, (to - from + 1), loadedData.size
                    (), from, to, headId, tailId);
        }

        for (Map.Entry<Long, Data> entry : loadedData.entrySet()) {
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

        if (this.store.isEnabled()) {
            tailId = store.getMaxItemId();
            headId = store.getMinItemId();

            loadBuffer();

            if (size() >= maxBufferSize) {
                bufferClosed = true;
            } else {
                bufferClosed = false;
            }

            scheduleResizing();
        } else {
            // disable queue item eviction logic
            maxBufferSize = Integer.MAX_VALUE;
        }
    }

    long nextId() {
        return ++tailId;
    }

    long shiftHead() {
        return headId++;
    }


    private void scheduleResizing() {
        service.scheduleResizing(name, 5000);
    }

    public boolean isEvictable() {
        if (!store.isEnabled()) {
            return false;
        }

        service.scheduleResizing(name, 5000);

        resizeBuffer(sizeAdviser.getRecommendedSize(name));
        return false;
    }

    protected void resizeBuffer(int newSize) {

        if (newSize <= 0) {
            return;
        }

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

            // buffer opened
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

                // buffer closed
            } else {

                if (newSize < maxBufferSize) {

                    if (newSize < buffSize) {

                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} drain closed buffer", name);

                        long to = headId + buffSize - 1;
                        long from = headId + newSize;

                        removeFromBuffer(from, to);
                    } else {

                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} new buffer size less then old, but closed buffer " +
                                    "still less then new buffer size. Nothing to drain. Full polled buffer will be " +
                                    "loaded automatically", name);
                    }
                } else {

                    long needMaximumToLoad = headId + buffSize + (newSize - maxBufferSize) - 1;

                    if (tailId >= needMaximumToLoad) {
                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} need load {} and NOT open buffer. " +
                                    "queue size = {} buffer.size() = {}", name, needMaximumToLoad, size(), buffer.size());

                        loadAll(headId + buffSize, needMaximumToLoad);

                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} after load buffer.size() = {}",
                                    name, buffer.size());
                    } else {
                        if (DEBUG_FULL || DEBUG_RESIZE)
                            logger.debug("resizeBuffer(): name = {} need load {} and OPEN buffer. " +
                                    "queue size = {} buffer.size() = {}", name, tailId - headId + 1, size(), buffer.size());

                        loadAll(headId + buffSize, tailId);
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


    public void clear() {
        store.clear();
        headId = 0;
        tailId = -1;
        heapCost = 0;
        buffer.clear();
        bufferClosed = false;
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

    public void destroy() {
    }

}

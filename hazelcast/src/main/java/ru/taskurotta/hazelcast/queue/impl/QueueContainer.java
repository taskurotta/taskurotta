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
import ru.taskurotta.hazelcast.ItemIdAware;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * This class contains methods be notable for the Queue.
 * such as pool,peek,clear..
 */
public class QueueContainer implements IdentifiedDataSerializable {

    private static final Logger logger = LoggerFactory.getLogger(QueueContainer.class);

    private static final int ID_PROMOTION_OFFSET = 100000;

    private CachedQueueConfig config;
    private QueueStoreWrapper store;
    private NodeEngine nodeEngine;
    private QueueService service;

    private final QueueWaitNotifyKey pollWaitNotifyKey;
    private final QueueWaitNotifyKey offerWaitNotifyKey;

    private String name;

    private ItemIdAware itemIdAware;


    // new stuff

    private volatile long headId = 0;
    private volatile long tailId = -1;
    private volatile boolean bufferClosed = false;

    private volatile int maxBufferSize;

    private Map<Long, QueueItem> buffer = new HashMap<Long, QueueItem>();


    public int size() {
        return (int) (tailId - headId + 1);
    }

    private boolean isEmpty() {
        return headId > tailId;
    }

    public long offer(Data data) {

        long itemId = nextId();

//        logger.severe("offer() itemId = " + itemId + " buffer.size() = " + buffer.size() + " headId = " + headId + " " +
//                "tailId = " + tailId + " bufferClosed = " + bufferClosed);

        store.store(itemId, data);

        if (!bufferClosed) {
            if (maxBufferSize > buffer.size()) {
//                logger.severe("offer() Add to buffer itemId = " + itemId);

                QueueItem item = new QueueItem(itemId, data);
                buffer.put(itemId, item);
            } else {
                bufferClosed = true;
            }
        }

        //cancelEvictionIfExists();
        return itemId;
    }


    public QueueItem poll() {

//        System.err.println("poll() headId = " + headId + " tailId = " + tailId + " buffer.size() " + buffer.size() +
//                " bufferClosed = " + bufferClosed);


        if (isEmpty()) {
            bufferClosed = false;
            return null;
        }

        if (buffer.isEmpty()) {
            loadBuffer();

            // open buffer if it is not full
            // todo: store may be broken and return no all set of items
            if (buffer.size() != maxBufferSize) {
                bufferClosed = false;
            }
        }


        long currHeadId = shiftHead();

//        logger.severe("poll() currHeadId = " + currHeadId);

        store.delete(currHeadId);

//        scheduleEvictionIfEmpty();

//        QueueItem item = buffer.remove(currHeadId);
//        if (item == null) {
//            logger.severe("RECEIVED NULL FROM BUFFER!");
//            try {
//                TimeUnit.SECONDS.sleep(1);
//            } catch (InterruptedException e) {
//                // ignore
//            }
//            System.exit(-1);
//        }
        return buffer.remove(currHeadId);
    }


    public void loadBuffer() {

        logger.trace("loading...");

        long maxId = headId + maxBufferSize - 1;

        if (maxId >= tailId) {
            maxId = tailId;
        }

        logger.trace("maxId = {} ", maxId);

        loadAll(headId, maxId);
    }

    private void loadAll(long from, long to) {

        logger.trace("before load: buffer size {} from = {} to = {}", buffer.size(), from, to);

        for (Map.Entry<Long, Data> entry : store.loadAll(from, to).entrySet()) {
            Long itemId = entry.getKey();
            QueueItem item = new QueueItem(itemId, entry.getValue());

            buffer.put(itemId, item);
        }

        logger.trace("after load: buffer size {} from = {} to = {}", buffer.size(), from, to);
    }

    private int getMaxBufferSize() {
        return ThreadLocalRandom.current().nextInt(config.getCacheSize()) + 1;
    }

    /**
     * initialization of head and tail id.
     * load date to the buffer
     */
    public void init(boolean fromBackup) {

        maxBufferSize = config.getCacheSize();

        // may be we have a bug on resize and loose items
        // more tests needed
        //resizeBuffer();

        scheduleEvictionIfEmpty();
//        if (!fromBackup && store.isEnabled()) {
//            // todo: OOM!
//            Set<Long> keys = store.loadAllKeys();
//            if (keys != null) {
//                long maxId = -1;
//                for (Long key : keys) {
//                    QueueItem item = new QueueItem(this, key, null);
//                    getItemQueue().offer(item);
//                    maxId = Math.max(maxId, key);
//                }
//                idGenerator = maxId + 1;
//            }
//        }
    }

    long nextId() {
        return ++tailId;
    }

    long shiftHead() {
        return headId++;
    }


    private void scheduleEvictionIfEmpty() {
        service.scheduleEviction(name, 5000);
    }

    public void cancelEvictionIfExists() {
    }

    public boolean isEvictable() {
        service.scheduleEviction(name, 5000);

        //resizeBuffer();
        return false;
    }

    private void resizeBuffer() {

        int buffSize = buffer.size();
        int newSize = getMaxBufferSize();

        logger.debug("resizeBuffer() oldSize = " + maxBufferSize + " newSize = " + newSize + " bufferSize = " +
                buffSize + " isEmpty = " + isEmpty());

        if (buffSize > maxBufferSize) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (maxBufferSize == newSize) {
            logger.debug("resizeBuffer() nothing to do");
            return;
        }

        if (!isEmpty()) {

            if (!bufferClosed) {

                if (newSize < buffSize) {

                    logger.debug("resizeBuffer() drain open buffer and close it");

                    long to = tailId;
                    long from = to - (buffSize - newSize);

                    removeFromBuffer(from, to);

                    bufferClosed = true;
                } else {

                    if (buffer.size() == newSize) {
                        logger.debug("resizeBuffer() close opened buffer");
                        bufferClosed = true;
                    } else {
                        logger.debug("resizeBuffer() nothing to do at this moment");
                    }
                }

            } else {

                if (newSize < buffSize) {

                    logger.debug("resizeBuffer() drain closed buffer");

                    long to = headId + buffSize - 1;
                    long from = headId + newSize;

                    removeFromBuffer(from, to);
                } else {

                    int needMaximumToLoad = newSize - buffSize;

                    if (headId + needMaximumToLoad < tailId) {
                        logger.debug("resizeBuffer() need load " + (needMaximumToLoad + 1) + " and NOT open buffer. " +
                                "queue size = " + size());

                        loadAll(headId, headId + needMaximumToLoad);
                        logger.debug("resizeBuffer() after load buffer.size() = " + buffer.size());
                    } else {
                        logger.debug("resizeBuffer() need load " + (tailId - headId + 1) + " and open buffer. " +
                                "queue size = " + size());

                        loadAll(headId, tailId);
                        logger.debug("resizeBuffer() after load buffer.size() = " + buffer.size());
                        bufferClosed = false;
                    }
                }
            }
        } else {
            logger.debug("resizeBuffer() nothing to do with empty queue");
        }


        maxBufferSize = newSize;
    }

    private void removeFromBuffer(long from, long to) {

        logger.debug("removeFromBuffer() from = " + from + " to = " + to + " q = " + (to - from + 1));

        int j = 0;

        for (long i = from; i <= to; i++) {
            if (buffer.remove(i) != null) {
                j++;
            }
        }

        logger.debug("removeFromBuffer() removed = " + j + " buffer.size() = " + buffer.size());
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

    public Map<Long, Data> clear() {
        throw new IllegalStateException("Not implemented yet!");
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

        // todo: needed methods should be added to CachedQueueStore directly
        if (store.isEnabled() && storeConfig.getStoreImplementation() instanceof ItemIdAware) {
            this.itemIdAware = (ItemIdAware) storeConfig.getStoreImplementation();
        }
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

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

import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.QueueStoreConfig;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.nio.serialization.SerializationService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.util.Clock;
import ru.taskurotta.hazelcast.ItemIdAware;
import ru.taskurotta.hazelcast.queue.impl.stats.LocalQueueStatsImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class contains methods be notable for the Queue.
 * such as pool,peek,clear..
 */
public class QueueContainer implements IdentifiedDataSerializable {
    private static final int ID_PROMOTION_OFFSET = 100000;

    private LinkedList<QueueItem> itemQueue;
    private final Map<Long, Data> dataMap = new HashMap<Long, Data>();

    private QueueConfig config;
    private QueueStoreWrapper store;
    private NodeEngine nodeEngine;
    private QueueService service;
    private ILogger logger;

    private long idGenerator;

    private final QueueWaitNotifyKey pollWaitNotifyKey;
    private final QueueWaitNotifyKey offerWaitNotifyKey;

    private String name;

    private long minAge = Long.MAX_VALUE;

    private long maxAge = Long.MIN_VALUE;

    private long totalAge;

    private long totalAgedCount;

    private boolean isEvictionScheduled;

    private ItemIdAware itemIdAware;


    // new stuff

    private volatile long headId = 0;
    private volatile long tailId = -1;
    private volatile boolean bufferClosed = false;

    private volatile int maxBufferSize;

    private Map<Long, QueueItem> buffer = new HashMap<Long, QueueItem>();

    // debug stuff

    AtomicInteger pollCounter = new AtomicInteger(0);
    AtomicInteger offerCounter = new AtomicInteger(0);

    public int size() {
        return (int) (tailId - headId + 1);
    }

    private boolean isEmpty() {
        return headId > tailId;
    }

    public long offer(Data data) {

        offerCounter.incrementAndGet();

        long itemId = nextId();

//        logger.severe("offer() itemId = " + itemId + " buffer.size() = " + buffer.size() + " headId = " + headId + " " +
//                "tailId = " + tailId + " bufferClosed = " + bufferClosed);
        store.store(itemId, data);

        if (!bufferClosed) {
            if (maxBufferSize > buffer.size()) {
//                logger.severe("offer() Add to buffer itemId = " + itemId);

                QueueItem item = new QueueItem(this, itemId, data);
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

//        System.err.println("loadBuffer()");

        long maxId = headId + maxBufferSize - 1;

//        System.err.println("loadBuffer() maxId = " + maxId);
        if (maxId >= tailId) {
            maxId = tailId;
        }

//        System.err.println("loadBuffer() maxId = " + maxId);

        loadAll(headId, maxId);
    }

    private void loadAll(long from, long to) {

//        System.err.println("loadAll from ($gte) " + from + " to ($lte) = " + to);

        Collection<Long> keys = new ArrayList<>();
        // $gte
        keys.add(from);
        // $lte
        keys.add(to);

        for (Map.Entry<Long, Data> entry : store.loadAll(keys).entrySet()) {
            Long itemId = entry.getKey();
            QueueItem item = new QueueItem(this, itemId, entry.getValue());

//            System.err.println("loadBuffer() add item = " + itemId);
            buffer.put(itemId, item);
        }
    }

    private int getMaxBufferSize() {
        return ThreadLocalRandom.current().nextInt(store.getMemoryLimit()) + 1;
    }

    /**
     * initialization of head and tail id.
     * load date to the buffer
     */
    public void init(boolean fromBackup) {

        resizeBuffer();

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

        resizeBuffer();
        return false;
    }

    private void resizeBuffer() {

        int buffSize = buffer.size();
        int newSize = getMaxBufferSize();

        logger.severe("resizeBuffer() oldSize = " + maxBufferSize + " newSize = " + newSize + " bufferSize = " +
                buffSize + " isEmpty = " + isEmpty());

        if (buffSize > maxBufferSize) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (maxBufferSize == newSize) {
            logger.severe("resizeBuffer() nothing to do");
            return;
        }

        if (!isEmpty()) {

            if (!bufferClosed) {

                if (newSize < buffSize) {

                    logger.severe("resizeBuffer() drain open buffer and close it");

                    long to = tailId;
                    long from = to - (buffSize - newSize);

                    removeFromBuffer(from, to);

                    bufferClosed = true;
                } else {

                    if (buffer.size() == newSize) {
                        logger.severe("resizeBuffer() close opened buffer");
                        bufferClosed = true;
                    } else {
                        logger.severe("resizeBuffer() nothing to do at this moment");
                    }
                }

            } else {

                if (newSize < buffSize) {

                    logger.severe("resizeBuffer() drain closed buffer");

                    long to = headId + buffSize - 1;
                    long from = headId + newSize;

                    removeFromBuffer(from, to);
                } else {

                    int needMaximumToLoad = newSize - buffSize;

                    if (headId + needMaximumToLoad < tailId) {
                        logger.severe("resizeBuffer() need load " + (needMaximumToLoad + 1) + " and NOT open buffer. " +
                                "queue size = " + size());

                        loadAll(headId, headId + needMaximumToLoad);
                        logger.severe("resizeBuffer() after load buffer.size() = " + buffer.size());
                    } else {
                        logger.severe("resizeBuffer() need load " + (tailId - headId + 1) + " and open buffer. " +
                                "queue size = " + size());

                        loadAll(headId, tailId);
                        logger.severe("resizeBuffer() after load buffer.size() = " + buffer.size());
                        bufferClosed = false;
                    }
                }
            }
        } else {
            logger.severe("resizeBuffer() nothing to do with empty queue");
        }


        maxBufferSize = newSize;
    }

    private void removeFromBuffer(long from, long to) {

        logger.severe("removeFromBuffer() from = " + from + " to = " + to + " q = " + (to - from + 1));

        int j = 0;

        for (long i = from; i <= to; i++) {
            if (buffer.remove(i) != null) {
                j++;
            }
        }

        logger.severe("removeFromBuffer() removed = " + j + " buffer.size() = " + buffer.size());
    }


    // old stuff

    public QueueContainer(String name) {
        this.name = name;
        pollWaitNotifyKey = new QueueWaitNotifyKey(name, "poll");
        offerWaitNotifyKey = new QueueWaitNotifyKey(name, "offer");
    }


    public QueueContainer(String name, QueueConfig config, NodeEngine nodeEngine, QueueService service) throws Exception {
        this(name);
        setConfig(config, nodeEngine, service);
    }




    public Map<Long, Data> addAll(Collection<Data> dataList) {
        Map<Long, Data> map = new HashMap<Long, Data>(dataList.size());
        List<QueueItem> list = new ArrayList<QueueItem>(dataList.size());
        for (Data data : dataList) {
            QueueItem item = new QueueItem(this, nextId(), null);
            if (!store.isEnabled() || store.getMemoryLimit() > getItemQueue().size()) {
                item.setData(data);
            }
            map.put(item.getItemId(), data);
            list.add(item);
        }
        if (store.isEnabled() && !map.isEmpty()) {
            try {
                store.storeAll(map);
            } catch (Exception e) {
                throw new HazelcastException(e);
            }
        }
        if (!list.isEmpty()) {
            getItemQueue().addAll(list);
            cancelEvictionIfExists();
        }
        return map;
    }

    public QueueItem peek() {
        QueueItem item = getItemQueue().peek();
        if (item == null) {
            return null;
        }
        if (store.isEnabled() && item.getData() == null) {
            try {
                load(item);
            } catch (Exception e) {
                throw new HazelcastException(e);
            }
        }
        return item;
    }


    public Map<Long, Data> drain(int maxSize) {
        int maxSizeParam = maxSize;
        if (maxSizeParam < 0 || maxSizeParam > getItemQueue().size()) {
            maxSizeParam = getItemQueue().size();
        }
        LinkedHashMap<Long, Data> map = new LinkedHashMap<Long, Data>(maxSizeParam);
        mapDrainIterator(maxSizeParam, map);
        if (store.isEnabled() && maxSizeParam != 0) {
            try {
                store.deleteAll(map.keySet());
            } catch (Exception e) {
                throw new HazelcastException(e);
            }
        }
        long current = Clock.currentTimeMillis();
        for (int i = 0; i < maxSizeParam; i++) {
            QueueItem item = getItemQueue().poll();
            //For Stats
            age(item, current);
        }
        if (maxSizeParam != 0) {
            scheduleEvictionIfEmpty();
        }
        return map;
    }

    public void mapDrainIterator(int maxSize, Map map) {
        Iterator<QueueItem> iter = getItemQueue().iterator();
        for (int i = 0; i < maxSize; i++) {
            QueueItem item = iter.next();
            if (store.isEnabled() && item.getData() == null) {
                try {
                    load(item);
                } catch (Exception e) {
                    throw new HazelcastException(e);
                }
            }
            map.put(item.getItemId(), item.getData());
        }
    }

    public Map<Long, Data> clear() {
        long current = Clock.currentTimeMillis();
        LinkedHashMap<Long, Data> map = new LinkedHashMap<Long, Data>(getItemQueue().size());
        for (QueueItem item : getItemQueue()) {
            map.put(item.getItemId(), item.getData());
            // For stats
            age(item, current);
        }
        if (store.isEnabled() && !map.isEmpty()) {
            try {
                store.deleteAll(map.keySet());
            } catch (Exception e) {
                throw new HazelcastException(e);
            }
        }
        getItemQueue().clear();
        dataMap.clear();
        scheduleEvictionIfEmpty();
        return map;
    }

    /**
     * iterates all items, checks equality with data
     * This method does not trigger store load.
     */
    public long remove(Data data) {
        Iterator<QueueItem> iter = getItemQueue().iterator();
        while (iter.hasNext()) {
            QueueItem item = iter.next();
            if (data.equals(item.getData())) {
                if (store.isEnabled()) {
                    try {
                        store.delete(item.getItemId());
                    } catch (Exception e) {
                        throw new HazelcastException(e);
                    }
                }
                iter.remove();
                //For Stats
                age(item, Clock.currentTimeMillis());
                scheduleEvictionIfEmpty();
                return item.getItemId();
            }
        }
        return -1;
    }

    /**
     * This method does not trigger store load.
     */
    public boolean contains(Collection<Data> dataSet) {
        for (Data data : dataSet) {
            boolean contains = false;
            for (QueueItem item : getItemQueue()) {
                if (item.getData() != null && item.getData().equals(data)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method triggers store load.
     */
    public List<Data> getAsDataList() {
        List<Data> dataList = new ArrayList<Data>(getItemQueue().size());
        for (QueueItem item : getItemQueue()) {
            if (store.isEnabled() && item.getData() == null) {
                try {
                    load(item);
                } catch (Exception e) {
                    throw new HazelcastException(e);
                }
            }
            dataList.add(item.getData());
        }
        return dataList;
    }

    /**
     * This method triggers store load
     */
    public Map<Long, Data> compareAndRemove(Collection<Data> dataList, boolean retain) {
        LinkedHashMap<Long, Data> map = new LinkedHashMap<Long, Data>();
        for (QueueItem item : getItemQueue()) {
            if (item.getData() == null && store.isEnabled()) {
                try {
                    load(item);
                } catch (Exception e) {
                    throw new HazelcastException(e);
                }
            }
            boolean contains = dataList.contains(item.getData());
            if ((retain && !contains) || (!retain && contains)) {
                map.put(item.getItemId(), item.getData());
            }
        }

        mapIterateAndRemove(map);

        return map;
    }

    public void mapIterateAndRemove(Map map) {
        if (map.size() <= 0) {
            return;
        }

        if (store.isEnabled()) {
            try {
                store.deleteAll(map.keySet());
            } catch (Exception e) {
                throw new HazelcastException(e);
            }
        }
        Iterator<QueueItem> iter = getItemQueue().iterator();
        while (iter.hasNext()) {
            QueueItem item = iter.next();
            if (map.containsKey(item.getItemId())) {
                iter.remove();
                //For Stats
                age(item, Clock.currentTimeMillis());
            }
        }
        scheduleEvictionIfEmpty();
    }

    private void load(QueueItem item) throws Exception {
        int bulkLoad = Math.min(getItemQueue().size(), store.getBulkLoad());
        bulkLoad = Math.min(getItemQueue().size(), bulkLoad);
        if (bulkLoad == 1) {
            item.setData(store.load(item.getItemId()));
        } else if (bulkLoad > 1) {
            Iterator<QueueItem> iter = getItemQueue().iterator();
            HashSet<Long> keySet = new HashSet<Long>(bulkLoad);
            for (int i = 0; i < bulkLoad; i++) {
                keySet.add(iter.next().getItemId());
            }
            Map<Long, Data> values = store.loadAll(keySet);

            int loaded = values.size();
            int shouldLoad = keySet.size();
            if (loaded != shouldLoad) {
                logger.warning("Store data load failed! loaded [" + loaded + "] out of[" + shouldLoad + "]. Possible data loss, Trigger queue cleanup...");
                triggerCleanup();
            }

            dataMap.putAll(values);
            item.setData(getDataFromMap(item.getItemId()));
        }
    }

    private void triggerCleanup() {
        if (itemIdAware != null) {
            long minId = itemIdAware.getMinItemId();
            logger.warning("Store cleanup: current queue id[" + idGenerator + "], min stored id[" + minId + "]");
            int cnt = 0;
            if (minId < 0) {//there are no backing collection
                cnt = getItemQueue().size();
                getItemQueue().clear();
            } else {
                Deque<QueueItem> queue = getItemQueue();
                QueueItem item = null;
                while ((item = queue.pollFirst()) != null
                        && (item.getItemId() < minId)) {
                    cnt++;
                }
                if (item != null) {
                    queue.offerFirst(item); //return polled item to queue
                }
            }
            logger.warning("Store cleanup: [" + cnt + "] items removed");
        } else {
            logger.warning("Cannot trigger store cleanup: ItemIdAware implementation is not set!");
        }
    }

    public boolean hasEnoughCapacity() {
        return hasEnoughCapacity(1);
    }

    public boolean hasEnoughCapacity(int delta) {
        return (getItemQueue().size() + delta) <= config.getMaxSize();
    }

    public Deque<QueueItem> getItemQueue() {
        if (itemQueue == null) {
            itemQueue = new LinkedList<QueueItem>();
        }
        return itemQueue;
    }

    public Data getDataFromMap(long itemId) {
        return dataMap.remove(itemId);
    }

    public void setConfig(QueueConfig config, NodeEngine nodeEngine, QueueService service) {
        this.nodeEngine = nodeEngine;
        this.service = service;
        this.logger = nodeEngine.getLogger(QueueContainer.class);
        this.config = new QueueConfig(config);
        // init queue store.
        final QueueStoreConfig storeConfig = config.getQueueStoreConfig();
        final SerializationService serializationService = nodeEngine.getSerializationService();
        this.store = QueueStoreWrapper.create(name, storeConfig, serializationService);

        if (store.isEnabled() && storeConfig.getStoreImplementation() instanceof ItemIdAware) {
            this.itemIdAware = (ItemIdAware) storeConfig.getStoreImplementation();
        }
    }


    void setId(long itemId) {
        idGenerator = Math.max(itemId + 1, idGenerator);
    }

    public QueueWaitNotifyKey getPollWaitNotifyKey() {
        return pollWaitNotifyKey;
    }

    public QueueWaitNotifyKey getOfferWaitNotifyKey() {
        return offerWaitNotifyKey;
    }

    public QueueConfig getConfig() {
        return config;
    }

    private void age(QueueItem item, long currentTime) {
        long elapsed = currentTime - item.getCreationTime();
        if (elapsed <= 0) {
            //elapsed time can not be a negative value, a system clock problem maybe. ignored
            return;
        }
        totalAgedCount++;
        totalAge += elapsed;

        minAge = Math.min(minAge, elapsed);
        maxAge = Math.max(maxAge, elapsed);
    }

    public void setStats(LocalQueueStatsImpl stats) {
        stats.setMinAge(minAge);
        stats.setMaxAge(maxAge);
        long totalAgedCountVal = Math.max(totalAgedCount, 1);
        stats.setAveAge(totalAge / totalAgedCountVal);
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(getItemQueue().size());
        for (QueueItem item : getItemQueue()) {
            out.writeObject(item);
        }
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        name = in.readUTF();
        int size = in.readInt();
        for (int j = 0; j < size; j++) {
            QueueItem item = in.readObject();
            getItemQueue().offer(item);
            setId(item.getItemId());
        }
    }

    public void destroy() {
        if (itemQueue != null) {
            itemQueue.clear();
        }
        dataMap.clear();
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

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

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.partition.InternalPartition;
import com.hazelcast.spi.EventService;
import com.hazelcast.spi.ManagedService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.RemoteService;
import com.hazelcast.util.ConcurrencyUtil;
import com.hazelcast.util.ConstructorFunction;
import com.hazelcast.util.scheduler.EntryTaskScheduler;
import com.hazelcast.util.scheduler.EntryTaskSchedulerFactory;
import com.hazelcast.util.scheduler.ScheduleType;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.LocalCachedQueueStats;
import ru.taskurotta.hazelcast.queue.config.CachedQueueConfig;
import ru.taskurotta.hazelcast.queue.config.CachedQueueServiceConfig;
import ru.taskurotta.hazelcast.queue.impl.proxy.QueueProxyImpl;
import ru.taskurotta.hazelcast.queue.impl.stats.LocalCachedQueueStatsImpl;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides important services via methods for the the Queue
 * such as {@link ru.taskurotta.hazelcast.queue.impl.QueueEvictionProcessor }
 */
public class QueueService implements ManagedService, RemoteService {
    /**
     * Service name.
     */
    public static final String SERVICE_NAME = CachedQueue.class.getName();

    private final EntryTaskScheduler queueEvictionScheduler;
    private final NodeEngine nodeEngine;
    private final ConcurrentMap<String, QueueContainer> containerMap
            = new ConcurrentHashMap<String, QueueContainer>();
    private final ConcurrentMap<String, LocalCachedQueueStatsImpl> statsMap
            = new ConcurrentHashMap<>(1000);
    private final ConstructorFunction<String, LocalCachedQueueStatsImpl> localQueueStatsConstructorFunction
            = new ConstructorFunction<String, LocalCachedQueueStatsImpl>() {
        @Override
        public LocalCachedQueueStatsImpl createNew(String key) {
            return new LocalCachedQueueStatsImpl();
        }
    };

    // todo: clear metrics on queue removing phase
    private SizeAdviser sizeAdviser;

    private final ILogger logger;

    public QueueService(NodeEngine nodeEngine) {
        this.nodeEngine = nodeEngine;
        ScheduledExecutorService defaultScheduledExecutor
                = nodeEngine.getExecutionService().getDefaultScheduledExecutor();
        QueueEvictionProcessor entryProcessor = new QueueEvictionProcessor(nodeEngine, this);
        this.queueEvictionScheduler = EntryTaskSchedulerFactory.newScheduler(
                defaultScheduledExecutor, entryProcessor, ScheduleType.POSTPONE);
        this.logger = nodeEngine.getLogger(QueueService.class);

        sizeAdviser = new SizeAdviser(nodeEngine.getHazelcastInstance().getName(), findQueueServiceConfig().getSizeConfig());
    }

    public SizeAdviser getSizeAdviser() {
        return sizeAdviser;
    }

    public void scheduleEviction(String name, long delay) {
        queueEvictionScheduler.schedule(delay, name, null);
    }

    public void cancelEviction(String name) {
        queueEvictionScheduler.cancel(name);
    }

    @Override
    public void init(NodeEngine nodeEngine, Properties properties) {
    }

    @Override
    public void reset() {
        containerMap.clear();
    }

    @Override
    public void shutdown(boolean terminate) {
        reset();
    }

    public QueueContainer getOrCreateContainer(final String name) throws Exception {
        QueueContainer container = containerMap.get(name);
        if (container != null) {
            return container;
        }

        container = new QueueContainer(name, findQueueConfig(name), nodeEngine, this);
        QueueContainer existing = containerMap.putIfAbsent(name, container);
        if (existing != null) {
            container = existing;
        } else {
            sizeAdviser.addQueue(name);
            container.init();
        }
        return container;
    }

    public CachedQueueServiceConfig findQueueServiceConfig() {
        return (CachedQueueServiceConfig) nodeEngine.getConfig()
                .getServicesConfig().getServiceConfig(QueueService.SERVICE_NAME);
    }

    public CachedQueueConfig findQueueConfig(String name) {
        return findQueueServiceConfig().findQueueConfig(name);
    }

    public void addContainer(String name, QueueContainer container) {
        containerMap.put(name, container);
    }

    // need for testing..
    public boolean containsQueue(String name) {
        return containerMap.containsKey(name);
    }

    @Override
    public QueueProxyImpl createDistributedObject(String objectId) {
        return new QueueProxyImpl(objectId, this, nodeEngine);
    }

    @Override
    public void destroyDistributedObject(String name) {
        containerMap.remove(name);
        sizeAdviser.removeQueue(name);
        nodeEngine.getEventService().deregisterAllListeners(SERVICE_NAME, name);
    }

    public boolean removeItemListener(String name, String registrationId) {
        EventService eventService = nodeEngine.getEventService();
        return eventService.deregisterListener(SERVICE_NAME, name, registrationId);
    }

    public NodeEngine getNodeEngine() {
        return nodeEngine;
    }

    public LocalCachedQueueStats createLocalQueueStats(String name, int partitionId) {
        LocalCachedQueueStatsImpl stats = getLocalQueueStatsImpl(name);
        QueueContainer container = containerMap.get(name);
        if (container == null) {
            return stats;
        }

        Address thisAddress = nodeEngine.getClusterService().getThisAddress();
        InternalPartition partition = nodeEngine.getPartitionService().getPartition(partitionId);

        Address owner = partition.getOwnerOrNull();
        if (thisAddress.equals(owner)) {
            stats.setCacheMaxSize(container.getCacheMaxSize());
            stats.setHeapCost(container.getHeapCost());
            stats.setCacheSize(container.getCacheSize());
        }

        return stats;
    }

    public LocalCachedQueueStatsImpl getLocalQueueStatsImpl(String name) {
        return ConcurrencyUtil.getOrPutIfAbsent(statsMap, name, localQueueStatsConstructorFunction);
    }

}

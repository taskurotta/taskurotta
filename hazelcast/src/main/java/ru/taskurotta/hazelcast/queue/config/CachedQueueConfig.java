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

package ru.taskurotta.hazelcast.queue.config;


public class CachedQueueConfig {

    /**
     * Default value of maximum size of Queue
     */
    public static final int DEFAULT_CACHE_SIZE = 0;
    /**
     * Default value of time to live for empty Queue
     */
    public static final int DEFAULT_EMPTY_QUEUE_TTL = -1;

    private String name;
    private int cacheSize = DEFAULT_CACHE_SIZE;
    private int emptyQueueTtl = DEFAULT_EMPTY_QUEUE_TTL;
    private CachedQueueStoreConfig queueStoreConfig;
    private boolean statisticsEnabled = true;

    public CachedQueueConfig() {
    }

    public CachedQueueConfig(CachedQueueConfig config) {
        this();
        this.name = config.name;
        this.cacheSize = config.cacheSize;
        this.emptyQueueTtl = config.emptyQueueTtl;
        this.statisticsEnabled = config.statisticsEnabled;
        this.queueStoreConfig = config.queueStoreConfig != null ? new CachedQueueStoreConfig(config.queueStoreConfig) :
                null;
    }

    public int getEmptyQueueTtl() {
        return emptyQueueTtl;
    }

    public void setEmptyQueueTtl(int emptyQueueTtl) {
        this.emptyQueueTtl = emptyQueueTtl;
    }

    public int getCacheSize() {
        return cacheSize == 0 ? Integer.MAX_VALUE : cacheSize;
    }

    public CachedQueueConfig setCacheSize(int cacheSize) {
        if (cacheSize < 0) {
            throw new IllegalArgumentException("Size of the queue can not be a negative value!");
        }
        this.cacheSize = cacheSize;
        return this;
    }


    public CachedQueueStoreConfig getQueueStoreConfig() {
        return queueStoreConfig;
    }

    public CachedQueueConfig setQueueStoreConfig(CachedQueueStoreConfig queueStoreConfig) {
        this.queueStoreConfig = queueStoreConfig;
        return this;
    }

    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    public CachedQueueConfig setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
        return this;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     * @return this queue config
     */
    public CachedQueueConfig setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QueueConfig{");
        sb.append("name='").append(name).append('\'');
        sb.append(", cacheSize=").append(cacheSize);
        sb.append(", emptyQueueTtl=").append(emptyQueueTtl);
        sb.append(", queueStoreConfig=").append(queueStoreConfig);
        sb.append(", statisticsEnabled=").append(statisticsEnabled);
        sb.append('}');
        return sb.toString();
    }
}

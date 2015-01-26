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

import ru.taskurotta.hazelcast.queue.QueueItemListener;

/**
 * Contains the configuration for an Item Listener.
 */
public class CachedQueueItemListenerConfig extends CachedQueueListenerConfig {

    private boolean includeValue = true;

    public CachedQueueItemListenerConfig() {
        super();
    }

    public CachedQueueItemListenerConfig(String className, boolean includeValue) {
        super(className);
        this.includeValue = includeValue;
    }

    public CachedQueueItemListenerConfig(QueueItemListener implementation, boolean includeValue) {
        super(implementation);
        this.includeValue = includeValue;
    }

    public CachedQueueItemListenerConfig(CachedQueueItemListenerConfig config) {
        includeValue = config.isIncludeValue();
        implementation = config.getImplementation();
        className = config.getClassName();
    }


    public QueueItemListener getImplementation() {
        return (QueueItemListener) implementation;
    }

    public CachedQueueItemListenerConfig setImplementation(final QueueItemListener implementation) {
        super.setImplementation(implementation);
        return this;
    }

    public boolean isIncludeValue() {
        return includeValue;
    }

    public CachedQueueItemListenerConfig setIncludeValue(boolean includeValue) {
        this.includeValue = includeValue;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CachedQueueItemListenerConfig");
        sb.append("{includeValue=").append(includeValue);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        CachedQueueItemListenerConfig that = (CachedQueueItemListenerConfig) o;

        if (includeValue != that.includeValue) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (includeValue ? 1 : 0);
        return result;
    }
}

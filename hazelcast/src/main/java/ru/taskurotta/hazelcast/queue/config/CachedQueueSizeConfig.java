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

/**
 * Configuration for queues capacity.
 * You can set a limit total memory cost of entries.
 */
public class CachedQueueSizeConfig {

    private SizePolicy sizePolicy = SizePolicy.FREE_HEAP_PERCENTAGE;

    private int size = 1;

    public CachedQueueSizeConfig() {
    }

    public CachedQueueSizeConfig(int size, SizePolicy sizePolicy) {
        setSize(size);
        this.sizePolicy = sizePolicy;

        if (sizePolicy != SizePolicy.FREE_HEAP_PERCENTAGE) {
            throw new IllegalArgumentException("Policy " + sizePolicy + " not implemented yet");
        }
    }

    public CachedQueueSizeConfig(CachedQueueSizeConfig config) {
        this.size = config.size;
        this.sizePolicy = config.sizePolicy;

        if (sizePolicy != SizePolicy.FREE_HEAP_PERCENTAGE) {
            throw new IllegalArgumentException("Policy " + sizePolicy + " not implemented yet");
        }
    }

    /**
     * Maximum Size Policy
     */
    public enum SizePolicy {
        /**
         * Decide maximum entry count according to node
         */
        PER_NODE,
        /**
         * Decide maximum size with use heap percentage
         */
        USED_HEAP_PERCENTAGE,
        /**
         * Decide maximum size with use heap size
         */
        USED_HEAP_SIZE,
        /**
         * Decide minimum free heap percentage to trigger cleanup
         */
        FREE_HEAP_PERCENTAGE,
        /**
         * Decide minimum free heap size to trigger cleanup
         */
        FREE_HEAP_SIZE
    }

    public int getSize() {
        return size;
    }

    public CachedQueueSizeConfig setSize(int size) {
        if (size > 0) {
            this.size = size;
        }
        return this;
    }

    public SizePolicy getSizePolicy() {
        return sizePolicy;
    }

    public CachedQueueSizeConfig setMaxSizePolicy(SizePolicy sizePolicy) {
        this.sizePolicy = sizePolicy;

        if (!sizePolicy.equals(SizePolicy.PER_NODE) && !sizePolicy.equals(SizePolicy.USED_HEAP_PERCENTAGE) ) {
            throw new IllegalArgumentException("Size policy " + sizePolicy.toString() + " not implemented yet");
        }

        return this;
    }


    @Override
    public String toString() {
        return "CachedQueueSizeConfig{"
                + "sizePolicy='" + sizePolicy
                + '\''
                + ", size=" + size
                + '}';
    }
}

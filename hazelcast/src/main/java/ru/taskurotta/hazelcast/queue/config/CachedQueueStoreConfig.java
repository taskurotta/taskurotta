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

import com.hazelcast.util.ValidationUtil;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStoreFactory;

import java.util.Properties;

/**
 * @author ali 12/14/12
 */
public class CachedQueueStoreConfig {

    public static final int DEFAULT_BATCH_LOAD_SIZE = 250;

    private boolean enabled = true;
    private boolean binary = false;
    private int batchLoadSize = DEFAULT_BATCH_LOAD_SIZE;
    private String objectClassName;
    private String className;
    private String factoryClassName;
    private Properties properties = new Properties();
    private CachedQueueStore storeImplementation;
    private CachedQueueStoreFactory factoryImplementation;

    public CachedQueueStoreConfig() {
    }

    public CachedQueueStoreConfig(CachedQueueStoreConfig config) {
        enabled = config.isEnabled();
        className = config.getClassName();
        storeImplementation = config.getStoreImplementation();
        factoryClassName = config.getFactoryClassName();
        factoryImplementation = config.getFactoryImplementation();
        properties.putAll(config.getProperties());
    }

    public CachedQueueStore getStoreImplementation() {
        return storeImplementation;
    }

    public CachedQueueStoreConfig setStoreImplementation(CachedQueueStore storeImplementation) {
        this.storeImplementation = storeImplementation;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CachedQueueStoreConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isBinary() {
        return binary;
    }

    public CachedQueueStoreConfig setBinary(boolean binary) {
        this.binary = binary;
        return this;
    }

    public int getBatchLoadSize() {
        return batchLoadSize;
    }

    public CachedQueueStoreConfig setBatchLoadSize(int batchLoadSize) {
        this.batchLoadSize = batchLoadSize;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public CachedQueueStoreConfig setClassName(String className) {
        this.className = className;
        return this;
    }

    public Properties getProperties() {
        return properties;
    }

    public CachedQueueStoreConfig setProperties(Properties properties) {
        ValidationUtil.isNotNull(properties, "properties");
        this.properties = properties;
        return this;
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    public CachedQueueStoreConfig setProperty(String name, String value) {
        properties.put(name, value);
        return this;
    }

    public String getFactoryClassName() {
        return factoryClassName;
    }

    public CachedQueueStoreConfig setFactoryClassName(String factoryClassName) {
        this.factoryClassName = factoryClassName;
        return this;
    }

    public CachedQueueStoreFactory getFactoryImplementation() {
        return factoryImplementation;
    }

    public CachedQueueStoreConfig setFactoryImplementation(CachedQueueStoreFactory factoryImplementation) {
        this.factoryImplementation = factoryImplementation;
        return this;
    }

    public String getObjectClassName() {
        return objectClassName;
    }

    public CachedQueueStoreConfig setObjectClassName(String objectClassName) {
        this.objectClassName = objectClassName;
        return this;
    }

    @Override
    public String toString() {
        return "CachedQueueStoreConfig{" +
                "enabled=" + enabled +
                ", binary=" + binary +
                ", batchLoadSize=" + batchLoadSize +
                ", objectClassName='" + objectClassName + '\'' +
                ", className='" + className + '\'' +
                ", factoryClassName='" + factoryClassName + '\'' +
                ", properties=" + properties +
                ", storeImplementation=" + storeImplementation +
                ", factoryImplementation=" + factoryImplementation +
                '}';
    }
}

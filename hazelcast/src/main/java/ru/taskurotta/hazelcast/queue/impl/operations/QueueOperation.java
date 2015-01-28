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

package ru.taskurotta.hazelcast.queue.impl.operations;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.PartitionAwareOperation;
import com.hazelcast.spi.exception.RetryableHazelcastException;
import ru.taskurotta.hazelcast.queue.impl.QueueContainer;
import ru.taskurotta.hazelcast.queue.impl.QueueDataSerializerHook;
import ru.taskurotta.hazelcast.queue.impl.QueueService;

import java.io.IOException;

/**
 * This class contains methods for Queue operations
 * such as {@link ru.taskurotta.hazelcast.queue.impl.operations.AddAllOperation}.
 */
public abstract class QueueOperation extends Operation
        implements PartitionAwareOperation, IdentifiedDataSerializable {

    protected String name;

    protected transient Object response;

    private transient QueueContainer container;

    protected QueueOperation() {
    }

    protected QueueOperation(String name) {
        this.name = name;
    }

    protected QueueOperation(String name, long timeoutMillis) {
        this.name = name;
        setWaitTimeout(timeoutMillis);
    }

    protected final QueueContainer getOrCreateContainer() {
        if (container == null) {
            QueueService queueService = getService();
            try {
                container = queueService.getOrCreateContainer(name);
            } catch (Exception e) {
                throw new RetryableHazelcastException(e);
            }
        }
        return container;
    }

    @Override
    public final Object getResponse() {
        return response;
    }

    @Override
    public final String getServiceName() {
        return QueueService.SERVICE_NAME;
    }

    public final String getName() {
        return name;
    }

    @Override
    public void afterRun() throws Exception {
    }

    @Override
    public void beforeRun() throws Exception {
    }

    @Override
    public boolean returnsResponse() {
        return true;
    }


    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        out.writeUTF(name);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        name = in.readUTF();
    }

    protected QueueService getQueueService() {
        return getService();
    }

    @Override
    public int getFactoryId() {
        return QueueDataSerializerHook.F_ID;
    }
}

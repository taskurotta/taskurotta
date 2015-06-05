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

import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spi.BackupAwareOperation;
import com.hazelcast.spi.Notifier;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.WaitNotifyKey;
import com.hazelcast.spi.WaitSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.impl.QueueDataSerializerHook;
import ru.taskurotta.hazelcast.queue.impl.QueueItem;
import ru.taskurotta.hazelcast.queue.impl.stats.LocalCachedQueueStatsImpl;

/**
 * Pool operation for Queue.
 */
public final class PollOperation extends QueueOperation
        implements WaitSupport, Notifier, IdentifiedDataSerializable, BackupAwareOperation {

    private static final Logger logger = LoggerFactory.getLogger(PollOperation.class);

    private QueueItem item;

    public PollOperation() {
    }

    public PollOperation(String name, long timeoutMillis) {
        super(name, timeoutMillis);
    }

    @Override
    public void run() {

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess run start detected");
        }

        item = getOrCreateContainer().poll();
        if (item != null) {
            response = item.getData();
        }

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess run end detected");
        }
    }

    @Override
    public void afterRun() throws Exception {

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess after run start detected");
        }

        LocalCachedQueueStatsImpl stats = getQueueService().getLocalQueueStatsImpl(name);
        if (response != null) {
            stats.incrementPolls();
        } else {
            stats.incrementEmptyPolls();
        }

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess after run end detected");
        }
    }

    @Override
    public boolean shouldNotify() {

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess shouldNotify detected");
        }

        return response != null;
    }

    @Override
    public WaitNotifyKey getNotifiedKey() {

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess getNotifiedKey detected");
        }

        return getOrCreateContainer().getOfferWaitNotifyKey();
    }

    @Override
    public WaitNotifyKey getWaitKey() {

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess getWaitKey detected");
        }

        return getOrCreateContainer().getPollWaitNotifyKey();
    }

    @Override
    public boolean shouldWait() {

        long waitTimeout = getWaitTimeout();
        int containerSize = getOrCreateContainer().size();

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess shouldWait detected waitTimeout = {}, containerSize = {}", waitTimeout, containerSize);
        }

        return waitTimeout != 0 && containerSize == 0;
    }

    @Override
    public void onWaitExpire() {

        if (name.equals("AbortProcess")) {
            logger.info("AbortProcess onWaitExpire detected");
        }

        getResponseHandler().sendResponse(null);
    }

    @Override
    public int getFactoryId() {
        return QueueDataSerializerHook.F_ID;
    }

    @Override
    public int getId() {
        return QueueDataSerializerHook.POLL;
    }

    @Override
    public boolean shouldBackup() {
        return false;
    }

    @Override
    public int getSyncBackupCount() {
        return 0;
    }

    @Override
    public int getAsyncBackupCount() {
        return 0;
    }

    @Override
    public Operation getBackupOperation() {
        return null;
    }
}

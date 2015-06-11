package ru.taskurotta.service.hz.recovery;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.service.hz.serialization.bson.DecisionBSerializer;
import ru.taskurotta.service.recovery.RecoveryService;
import ru.taskurotta.service.recovery.RecoveryThreads;
import ru.taskurotta.transport.model.Decision;
import ru.taskurotta.util.Shutdown;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * User: stukushin
 * Date: 11.06.2015
 * Time: 14:52
 */

public class IncompleteTaskFinder implements RecoveryThreads {

    private static final Logger logger = LoggerFactory.getLogger(IncompleteTaskFinder.class);

    private RecoveryService recoveryService;
    private long incompleteTaskFindTimeout;
    private int batchSize;
    private Lock nodeLock;

    private AtomicBoolean enabled = new AtomicBoolean(false);
    private ScheduledExecutorService executorService;
    private DBCollection decisionDBCollection;

    private static final String RECOVERY_TIME_NAME = DecisionBSerializer.RECOVERY_TIME.toString();

    public IncompleteTaskFinder(RecoveryService recoveryService, DB mongoDB, String decisionCollectionName,
                                long incompleteTaskFindTimeout, int batchSize, Lock  nodeLock, boolean enabled) {
        this.recoveryService = recoveryService;
        this.incompleteTaskFindTimeout = incompleteTaskFindTimeout;
        this.batchSize = batchSize;
        this.nodeLock = nodeLock;

        this.decisionDBCollection = mongoDB.getCollection(decisionCollectionName);

        this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread("IncompleteTaskFinder");
            }
        });
        Shutdown.addHook(executorService);

        if (enabled) {
            start();
        } else {
            logger.warn("Recovery service IncompleteTaskFinder is disabled.");
        }
    }

    @Override
    public void start() {
        if (!enabled.compareAndSet(false, true)) {
            // already started
            return;
        }

        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (nodeLock.tryLock()) {

                    BasicDBObject query = new BasicDBObject(RECOVERY_TIME_NAME, new BasicDBObject("$lte", System.currentTimeMillis()));
                    try (DBCursor dbCursor = decisionDBCollection.find(query).batchSize(batchSize)) {
                        while (dbCursor.hasNext()) {
                            DBObject dbObject = dbCursor.next();
                            Decision decision = (Decision) ((DBObjectCheat) dbObject).getObject();

                            recoveryService.resurrectTask(decision.getTaskId(), decision.getProcessId());
                        }
                    }

                }
            }
        }, 0l, incompleteTaskFindTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        enabled.set(false);
    }

    @Override
    public boolean isStarted() {
        return enabled.get();
    }
}

package ru.taskurotta.service.hz.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.spring.mongodb.MongoDBConverter;
import com.hazelcast.spring.mongodb.SpringMongoDBConverter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.IncompleteProcessFinder;
import ru.taskurotta.service.recovery.RecoveryOperation;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 27.12.13
 * Time: 12:17
 */
public class MongoIncompleteProcessFinder implements IncompleteProcessFinder {

    private static final Logger logger = LoggerFactory.getLogger(MongoIncompleteProcessFinder.class);

    private OperationExecutor operationExecutor;

    private MongoDBConverter converter;

    private static final String START_TIME_INDEX_NAME = "startTime";
    private static final String STATE_INDEX_NAME = "state";

    public MongoIncompleteProcessFinder(final HazelcastInstance hazelcastInstance, final OperationExecutor operationExecutor,
                                        final MongoTemplate mongoTemplate, final String processesStorageMapName,
                                        final long findIncompleteProcessPeriod, final long incompleteTimeOutMillis,
                                        final String mongoRecoveryLockName, boolean enabled) {

        if (!enabled) {
            return;
        }

        this.operationExecutor = operationExecutor;
        this.converter = new SpringMongoDBConverter(mongoTemplate);

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("MongoIncompleteProcessFinder");
                return thread;
            }
        });
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!operationExecutor.isEmpty()) {
                        logger.debug("RecoveryOperationExecutor queue isn't empty. Skip find incomplete processes");
                        return;
                    }

                    ILock iLock = hazelcastInstance.getLock(mongoRecoveryLockName);
                    if (iLock.tryLock()) {
                        logger.debug("Get lock for find incomplete processes");
                    } else {
                        logger.debug("Can't get lock for find incomplete processes");
                        return;
                    }

                    long timeBefore = System.currentTimeMillis() - incompleteTimeOutMillis;

                    if (logger.isDebugEnabled()) {
                        logger.debug("Try to find incomplete processes, started before [{}]", new Date(timeBefore));
                    }

                    DBCollection dbCollection = mongoTemplate.getCollection(processesStorageMapName);

                    BasicDBObject query = new BasicDBObject();
                    query.append(START_TIME_INDEX_NAME, new BasicDBObject("$lte", timeBefore));
                    query.append(STATE_INDEX_NAME, 0);

                    int counter = 0;
                    try (DBCursor dbCursor = dbCollection.find(query)) {
                        while (dbCursor.hasNext()) {
                            DBObject dbObject = dbCursor.next();
                            Process process = (Process) converter.toObject(Process.class, dbObject);
                            UUID processId = process.getProcessId();
                            toRecovery(processId);

                            counter++;
                        }
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Found [{}] incomplete processes", counter);
                    }
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        }, 0l, findIncompleteProcessPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void toRecovery(UUID processId) {
        operationExecutor.enqueue(new RecoveryOperation(processId));
        logger.trace("Send process [{}] to recovery", processId);
    }
}

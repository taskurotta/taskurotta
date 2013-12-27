package ru.taskurotta.service.ora.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.IncompleteProcessFinder;
import ru.taskurotta.service.recovery.RecoveryOperation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: stukushin
 * Date: 18.12.13
 * Time: 15:48
 */
public class OraIncompleteProcessFinder implements IncompleteProcessFinder {

    private static final Logger logger = LoggerFactory.getLogger(OraIncompleteProcessFinder.class);

    private OperationExecutor operationExecutor;

    private static final String SQL_FIND_INCOMPLETE_PROCESSES =
            "SELECT process_id FROM process WHERE state = ? AND start_time < ? ORDER BY start_time";

    public OraIncompleteProcessFinder(final DataSource dataSource, final HazelcastInstance hazelcastInstance,
                                      final OperationExecutor operationExecutor, final long findIncompleteProcessPeriod,
                                      final long incompleteTimeOutMillis, final String recoveryLockName,
                                      boolean enabled) {

        if (!enabled) {
            return;
        }

        this.operationExecutor = operationExecutor;

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("OraIncompleteProcessFinderThread");
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

                    ILock iLock = hazelcastInstance.getLock(recoveryLockName);
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

                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_INCOMPLETE_PROCESSES)) {

                        preparedStatement.setInt(1, 0);
                        preparedStatement.setLong(2, timeBefore);

                        ResultSet resultSet = preparedStatement.executeQuery();
                        while (resultSet.next()) {
                            UUID processId = UUID.fromString(resultSet.getString("process_id"));
                            toRecovery(processId);
                        }
                    } catch (SQLException ex) {
                        throw new IllegalStateException("Database error", ex);
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

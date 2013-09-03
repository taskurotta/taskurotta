package ru.taskurotta.recovery;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 15.08.13
 * Time: 16:42
 */
public class ProcessAnalyzer {

    private static Logger logger = LoggerFactory.getLogger(ProcessAnalyzer.class);

    private DataSource dataSource;
    private HazelcastInstance hazelcastInstance;
    private String analyzeProcessQueueName;

    private int processBatchSize = 1000;

    public ProcessAnalyzer(DataSource dataSource, HazelcastInstance hazelcastInstance, String analyzeProcessQueueName) {
        logger.info("Create ProcessAnalyzer");

        this.dataSource = dataSource;
        this.hazelcastInstance = hazelcastInstance;
        this.analyzeProcessQueueName = analyzeProcessQueueName;
    }

    public void init() {
        logger.debug("Try to find incomplete processes");

        IQueue<UUID> analyzeProcessQueue = hazelcastInstance.getQueue(analyzeProcessQueueName);

        if (!analyzeProcessQueue.isEmpty()) {
            logger.debug("Queue of analyzing process isn't empty");

            return;
        }

        Collection<UUID> processIds = findIncompleteProcesses();

        if (processIds == null || processIds.isEmpty()) {
            logger.debug("Collection of analyzing process ids is empty");

            return;
        }

        analyzeProcessQueue.addAll(processIds);

        if (logger.isInfoEnabled()) {
            logger.info("Add [{}] process ids for analyze", processIds.size());
        }
    }

    private Collection<UUID> findIncompleteProcesses() {
        Collection<UUID> processIds = new ArrayList<>();

        long fromTime = System.currentTimeMillis();

        if (logger.isInfoEnabled()) {
            logger.info("Try to find incomplete processes, was started before [{} ({})]", fromTime, new Date(fromTime));
        }

        String query = "SELECT * FROM (SELECT process_id FROM process WHERE state = ? AND start_time < ? ORDER BY start_time) WHERE ROWNUM <= ?";

        int incompleteProcessCount = 0;

        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, 0);
            preparedStatement.setLong(2, fromTime);
            preparedStatement.setInt(3, processBatchSize);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                incompleteProcessCount++;

                UUID processId = UUID.fromString(resultSet.getString("process_id"));

                processIds.add(processId);

                logger.debug("Found incomplete processId [{}]", processId);
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }

        logger.info("Found [{}] incomplete processes", incompleteProcessCount);

        return processIds;
    }

    public void setProcessBatchSize(int processBatchSize) {
        this.processBatchSize = processBatchSize;
    }
}

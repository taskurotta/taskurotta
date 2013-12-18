package ru.taskurotta.service.ora.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.recovery.IncompleteProcessFinder;

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
 * Date: 18.12.13
 * Time: 15:48
 */
public class OraIncompleteProcessFinder implements IncompleteProcessFinder {

    private static final Logger logger = LoggerFactory.getLogger(OraIncompleteProcessFinder.class);

    private DataSource dataSource;
    private int batchSize;

    private static final String SQL_FIND_INCOMPLETE_PROCESSES =
            "SELECT * FROM (SELECT process_id FROM process WHERE state = ? AND start_time < ? ORDER BY start_time) WHERE ROWNUM <= ?";

    public OraIncompleteProcessFinder(DataSource dataSource, int batchSize) {
        this.dataSource = dataSource;
        this.batchSize = batchSize;
    }

    @Override
    public Collection<UUID> find(long inactiveTimeOutMillis) {
        long fromTime = System.currentTimeMillis() - inactiveTimeOutMillis;

        if (logger.isInfoEnabled()) {
            logger.info("Try to find incomplete processes, was started before [{} ({})]", fromTime, new Date(fromTime));
        }

        Collection<UUID> processIds = new ArrayList<>();

        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_INCOMPLETE_PROCESSES)) {

            preparedStatement.setInt(1, 0);
            preparedStatement.setLong(2, fromTime);
            preparedStatement.setInt(3, batchSize);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                UUID processId = UUID.fromString(resultSet.getString("process_id"));

                processIds.add(processId);

                logger.debug("Found incomplete processId [{}]", processId);
            }

        } catch (SQLException ex) {
            throw new IllegalStateException("Database error", ex);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Found [{}] incomplete processes", processIds.size());
        }

        return processIds;
    }
}

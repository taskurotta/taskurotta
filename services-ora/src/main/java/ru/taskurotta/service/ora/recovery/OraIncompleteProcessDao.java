package ru.taskurotta.service.ora.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.recovery.IncompleteProcessDao;
import ru.taskurotta.service.recovery.IncompleteProcessesCursor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Oracle implementation for incomplete processes
 * Date: 13.01.14 11:10
 */
public class OraIncompleteProcessDao implements IncompleteProcessDao {

    protected static final String SQL_FIND_INCOMPLETE_PROCESSES =
            "SELECT process_id FROM process WHERE state = ? AND start_time < ?";

    private static final Logger logger = LoggerFactory.getLogger(OraIncompleteProcessDao.class);

    private DataSource dataSource;

    public OraIncompleteProcessDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public IncompleteProcessesCursor findProcesses(long timeBefore, int limit) {
        final Collection<UUID> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(getSql(limit))) {

            preparedStatement.setInt(1, Process.START);
            preparedStatement.setLong(2, timeBefore);
            if (limit>0) {
                preparedStatement.setInt(3, limit);
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                UUID processId = UUID.fromString(resultSet.getString("process_id"));
                result.add(processId);
            }

        } catch (Throwable e) {
            logger.error("Cannot find incomplete before time["+timeBefore+"]processes limit["+limit+"] due to database error", e);
            throw new ServiceCriticalException("Incomplete processes search before time["+timeBefore+"] failed", e);
        }
        return new IncompleteProcessesCursor() {
            @Override
            public Collection<UUID> getNext() {
                return result;
            }
        };
    }

    private String getSql(int limit) {
        return limit>0? SQL_FIND_INCOMPLETE_PROCESSES + " AND ROWNUM < ? ": SQL_FIND_INCOMPLETE_PROCESSES;
    }

}

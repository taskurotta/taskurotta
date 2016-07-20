package ru.taskurotta.service.pg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.common.ResultSetCursor;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public abstract class UuidResultSetCursor implements ResultSetCursor<UUID> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected Connection connection;
    protected PreparedStatement preparedStatement;
    protected ResultSet resultSet;
    protected int limit;
    protected String fieldName;

    private DataSource dataSource;

    public UuidResultSetCursor(DataSource dataSource, String fieldName, int limit) {
        this.fieldName =fieldName;
        this.limit = limit;
        this.dataSource = dataSource;
    }

    private void init() throws SQLException {
        connection = dataSource.getConnection();
        preparedStatement = prepareStatement(connection);
        resultSet = preparedStatement.executeQuery();
    }

    protected abstract PreparedStatement prepareStatement(Connection conn) throws SQLException;

    protected abstract String constructErrorMessage(Throwable e);

    @Override
    public Collection<UUID> getNext() {
        Collection<UUID> result = new ArrayList<>(limit);
        try {
            if (resultSet == null) {
                init();
            }

            int i = 0;
            while (i++ < limit && resultSet.next()) {
                result.add(UUID.fromString(resultSet.getString(fieldName)));
            }
        } catch (SQLException e) {
            String message = constructErrorMessage(e);
            logger.error(message, e);
            throw new ServiceCriticalException(message, e);
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                throw new ServiceCriticalException("Error on closing ResultSet", e);
            }
        }

        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                throw new ServiceCriticalException("Error on closing PreparedStatement", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new ServiceCriticalException("Error on closing Connection", e);
            }
        }
    }

}

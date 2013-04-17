package ru.taskurotta.backend.ora.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * User: greg
 */
public final class SqlResourceCloser {

    private static final Logger logger = LoggerFactory.getLogger(SqlResourceCloser.class);

    private SqlResourceCloser() {

    }

    public static void closeResources(Object... resources) {
        for (Object obj : resources) {
            if (obj instanceof Connection) {
                closeConnection((Connection) obj);
            } else if (obj instanceof Statement) {
                closeStatement((Statement) obj);
            }
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Fatal db error", e);
            }
        }
    }

    public static void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error("Fatal db error", e);
            }
        }
    }
}

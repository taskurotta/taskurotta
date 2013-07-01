package ru.taskurotta.backend.ora.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: greg
 */
public final class SqlResourceCloser {

    private static final Logger logger = LoggerFactory.getLogger(SqlResourceCloser.class);

    private SqlResourceCloser() {

    }

    public static void closeResources(AutoCloseable... resources) {
        for (AutoCloseable obj : resources) {
            try {
                obj.close();
            } catch (Exception e) {
                logger.error("Error close resource", e);
            }
        }
    }
}

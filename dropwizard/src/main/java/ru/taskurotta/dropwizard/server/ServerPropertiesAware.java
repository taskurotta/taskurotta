package ru.taskurotta.dropwizard.server;

import java.util.Properties;

/**
 * Server would inject properties to the beans implementing this interface
 * Date: 19.12.13 11:36
 */
public interface ServerPropertiesAware {

    void setProperties(Properties properties);

}

package ru.taskurotta.console.internal.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 17.05.13 16:08
 */
public class ConsoleConfig extends Configuration {

    @JsonProperty
    private String contextLocation;

    @JsonProperty
    private Properties properties;

    public String getContextLocation() {
        return contextLocation;
    }

    public void setContextLocation(String contextLocation) {
        this.contextLocation = contextLocation;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

}

package ru.taskurotta.dropwizard.server.application;

import com.bazaarvoice.dropwizard.assets.AssetsBundleConfiguration;
import com.bazaarvoice.dropwizard.assets.AssetsConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import ru.taskurotta.service.config.impl.MemoryConfigService;

import java.util.Properties;

/**
 * Created on 22.01.2015.
 */
public class TaskServerConfig extends Configuration implements AssetsBundleConfiguration {

    @JsonProperty
    private final AssetsConfiguration assets = new AssetsConfiguration();

    @JsonProperty
    private Properties properties;

    @JsonProperty
    private MemoryConfigService actorConfig;

    @JsonProperty
    private String contextLocation;

    @JsonProperty
    private String[] resourceBeans;

    @JsonProperty
    private String[] healthCheckBeans;

    @JsonProperty
    private String jerseyUrlPattern = "/rest/*";

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getContextLocation() {
        return contextLocation;
    }

    public void setContextLocation(String contextLocation) {
        this.contextLocation = contextLocation;
    }

    public MemoryConfigService getActorConfig() {
        return actorConfig;
    }

    public void setActorConfig(MemoryConfigService actorConfig) {
        this.actorConfig = actorConfig;
    }

    public String[] getResourceBeans() {
        return resourceBeans;
    }

    public String[] getHealthCheckBeans() {
        return healthCheckBeans;
    }

    public String getJerseyUrlPattern() {
        return jerseyUrlPattern;
    }

    @Override
    public AssetsConfiguration getAssetsConfiguration() {
        return assets;
    }

}

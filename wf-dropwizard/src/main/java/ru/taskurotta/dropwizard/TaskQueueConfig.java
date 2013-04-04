package ru.taskurotta.dropwizard;

import java.util.Properties;

import org.hibernate.validator.constraints.NotEmpty;

import ru.taskurotta.backend.config.impl.MemoryConfigBackend;
import ru.taskurotta.dropwizard.internal.pooling.InternalPoolConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

public class TaskQueueConfig extends Configuration {
	
	@JsonProperty
	private Properties properties;
	
	@JsonProperty
	private MemoryConfigBackend actorConfig;
	
	@JsonProperty
	private InternalPoolConfig internalPoolConfig;
	
	@NotEmpty
	@JsonProperty
	private String contextLocation;
	
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
	
	public InternalPoolConfig getInternalPoolConfig() {
		return internalPoolConfig;
	}

	public void setInternalPoolConfig(InternalPoolConfig internalPoolConfig) {
		this.internalPoolConfig = internalPoolConfig;
	}

	public MemoryConfigBackend getActorConfig() {
		return actorConfig;
	}

	public void setActorConfig(MemoryConfigBackend actorConfig) {
		this.actorConfig = actorConfig;
	}
	
}

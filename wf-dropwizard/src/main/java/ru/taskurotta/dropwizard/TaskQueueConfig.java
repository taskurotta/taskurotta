package ru.taskurotta.dropwizard;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

public class TaskQueueConfig extends Configuration {
	
	@JsonProperty
	private Properties properties;

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
	
}

package ru.taskurotta.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

public class TaskQueueConfig extends Configuration {
	
	@NotEmpty
	@JsonProperty
	private String contextLocation;

	public String getContextLocation() {
		return contextLocation;
	}

	public void setContextLocation(String contextLocation) {
		this.contextLocation = contextLocation;
	}
	
}

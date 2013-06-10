package ru.fccland.wf.ws.usersession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by void 15.03.13 12:48
 */
public class UserSessionConfiguration extends Configuration {

	@NotEmpty
	@JsonProperty
	private String defaultRole = "user";

	public String getDefaultRole() {
		return defaultRole;
	}

}

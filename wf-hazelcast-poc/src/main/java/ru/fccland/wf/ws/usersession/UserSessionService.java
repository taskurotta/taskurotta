package ru.fccland.wf.ws.usersession;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

/**
 * Created by void 15.03.13 12:49
 */
public class UserSessionService extends Service<UserSessionConfiguration> {
	@Override
	public void initialize(Bootstrap<UserSessionConfiguration> bootstrap) {
		bootstrap.setName("user-session");
	}

	@Override
	public void run(UserSessionConfiguration userSessionConfiguration, Environment environment) throws Exception {
		environment.addResource(new UserSessionResource());
		environment.addHealthCheck(new DummyHealthCheck("Mighty-mouse"));
	}

}

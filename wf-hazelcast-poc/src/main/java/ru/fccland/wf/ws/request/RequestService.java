package ru.fccland.wf.ws.request;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import ru.fccland.wf.ws.usersession.DummyHealthCheck;
import ru.fccland.wf.ws.usersession.UserSessionResource;

/**
 * Created by void 15.03.13 18:49
 */
public class RequestService extends Service<RequestConfiguration> {
	@Override
	public void initialize(Bootstrap<RequestConfiguration> bootstrap) {
		bootstrap.setName("GKN request");
	}

	@Override
	public void run(RequestConfiguration configuration, Environment environment) throws Exception {
		environment.addResource(new RequestResource());
	}
}

package ru.fccland.wf.ws.usersession;

import com.yammer.metrics.core.HealthCheck;

/**
 * Created by void 15.03.13 17:07
 */
public class DummyHealthCheck extends HealthCheck {

	protected DummyHealthCheck(String name) {
		super(name);
	}

	@Override
	protected Result check() throws Exception {
		return Result.healthy();
	}
}

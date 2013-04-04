package ru.taskurotta.dropwizard.internal;

import ru.taskurotta.backend.config.ConfigBackend;

public interface ConfigBackendAware {
	
	public void setConfigBackend(ConfigBackend config);
	
}

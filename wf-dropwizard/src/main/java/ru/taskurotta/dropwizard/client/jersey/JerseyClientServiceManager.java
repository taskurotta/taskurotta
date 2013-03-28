package ru.taskurotta.dropwizard.client.jersey;

import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.client.internal.DeciderClientProviderCommon;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.server.TaskServer;

public class JerseyClientServiceManager implements ClientServiceManager {

	private TaskServer taskServer;

	@Override
	public DeciderClientProvider getDeciderClientProvider() {
		return new DeciderClientProviderCommon(taskServer);
	}

	@Override
	public TaskSpreaderProvider getTaskSpreaderProvider() {
		return new TaskSpreaderProviderCommon(taskServer);
	}

	@Required
	public void setTaskServer(TaskServer taskServer) {
		this.taskServer = taskServer;
	}

}

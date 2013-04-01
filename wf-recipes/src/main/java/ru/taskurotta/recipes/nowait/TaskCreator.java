package ru.taskurotta.recipes.nowait;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.nowait.decider.NoWaitDeciderClient;

/**
 * Created by void 27.03.13 20:22
 */
public class TaskCreator {
	private ClientServiceManager clientServiceManager;

	public void createStartTask() {
		DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
		NoWaitDeciderClient nowaitDecider = deciderClientProvider.getDeciderClient(NoWaitDeciderClient.class);
		nowaitDecider.start();
	}

	public void setClientServiceManager(ClientServiceManager clientServiceManager) {
		this.clientServiceManager = clientServiceManager;
	}
}

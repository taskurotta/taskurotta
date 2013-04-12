package ru.taskurotta.recipes.summator;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.summator.decider.SummatorDeciderClient;
import ru.taskurotta.test.FlowArbiterFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by void 05.04.13 18:54
 */
public class TaskCreator {
    public static final int ARRAY_SIZE = 250;
    public static final int MAX_VALUE = 100;
    private ClientServiceManager clientServiceManager;

	public void createStartTask() {
		DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
		SummatorDeciderClient summator = deciderClientProvider.getDeciderClient(SummatorDeciderClient.class);

		List<Integer> data = initData(ARRAY_SIZE, MAX_VALUE);

        summator.start(data);

		int testResult = 0;
		for (int i : data) {
			testResult += i;
		}

        ArbiterProfilerImpl arbiter = (ArbiterProfilerImpl) new FlowArbiterFactory().getInstance(); // created in spring context
        arbiter.setTestResult(testResult);
	}

	private List<Integer> initData(int size, int maxValue) {
        List<Integer> data = new ArrayList<Integer>(size);
		for (int i=0; i < size; i++) {
			data.add((int)(Math.random() * maxValue));
		}
		return data;
	}

	public void setClientServiceManager(ClientServiceManager clientServiceManager) {
		this.clientServiceManager = clientServiceManager;
	}
}

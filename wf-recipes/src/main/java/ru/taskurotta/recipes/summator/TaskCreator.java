package ru.taskurotta.recipes.summator;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.recipes.summator.decider.SummatorDeciderClient;
import ru.taskurotta.test.flow.FlowArbiterFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by void 05.04.13 18:54
 */
public class TaskCreator {
    public static final int ARRAY_SIZE = 5;
    public static final int MAX_VALUE = 100;
    private ClientServiceManager clientServiceManager;
    private int arraySize = ARRAY_SIZE;
    private int maxValue = MAX_VALUE;

	public void createStartTask() {
		DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
		SummatorDeciderClient summator = deciderClientProvider.getDeciderClient(SummatorDeciderClient.class);

		List<Integer> data = initData(arraySize, maxValue);

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

    public void setArraySize(int arraySize) {
        this.arraySize = arraySize;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
}

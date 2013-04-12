package ru.taskurotta.recipes.summator;

import ru.taskurotta.test.BasicFlowArbiter;

import java.util.List;

/**
 * Created by void 08.04.13 16:29
 */
public class ArbiterProfilerImpl extends BasicFlowArbiter {

    private int testResult;

    public ArbiterProfilerImpl(List<String> stages) {
        super(stages);
    }

    public int getTestResult() {
        return testResult;
    }

    public void setTestResult(int testResult) {
        this.testResult = testResult;
    }
}

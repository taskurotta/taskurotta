package ru.taskurotta.dropwizard.client.serialization.wrapper;

import ru.taskurotta.transport.model.DecisionContainer;

public class DecisionContainerWrapper {

    private DecisionContainer decisionContainer;

    public DecisionContainerWrapper() {
    }

    public DecisionContainerWrapper(DecisionContainer decisionContainer) {
        this.decisionContainer = decisionContainer;
    }

    public DecisionContainer getResultContainer() {
        return decisionContainer;
    }

    public void setResultContainer(DecisionContainer decisionContainer) {
        this.decisionContainer = decisionContainer;
    }

    @Override
    public String toString() {
        return "ResultContainerWrapper [resultContainer=" + decisionContainer + "]";
    }

}

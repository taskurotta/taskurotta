package ru.taskurotta.client.serialization.wrapper;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ru.taskurotta.client.serialization.ResultContainerDeserializer;
import ru.taskurotta.server.transport.DecisionContainer;

public class ResultContainerWrapper {
	
	private DecisionContainer decisionContainer;

	public ResultContainerWrapper() {
	}
	
	public ResultContainerWrapper(DecisionContainer decisionContainer) {
		this.decisionContainer = decisionContainer;
	}	
	
	public DecisionContainer getDecisionContainer() {
		return decisionContainer;
	}

	@JsonDeserialize(using=ResultContainerDeserializer.class)
	public void setDecisionContainer(DecisionContainer decisionContainer) {
		this.decisionContainer = decisionContainer;
	}

	@Override
	public String toString() {
		return "DecisionContainerWrapper [decisionContainer=" + decisionContainer + "]";
	}
	
}

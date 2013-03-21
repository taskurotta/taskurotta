package ru.taskurotta.client.serialization.wrapper;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ru.taskurotta.client.serialization.ResultContainerDeserializer;
import ru.taskurotta.server.transport.ResultContainer;

public class ResultContainerWrapper {
	
	private ResultContainer resultContainer;

	public ResultContainerWrapper() {
	}
	
	public ResultContainerWrapper(ResultContainer resultContainer) {
		this.resultContainer = resultContainer;
	}	
	
	public ResultContainer getResultContainer() {
		return resultContainer;
	}

	@JsonDeserialize(using=ResultContainerDeserializer.class)
	public void setResultContainer(ResultContainer resultContainer) {
		this.resultContainer = resultContainer;
	}

	@Override
	public String toString() {
		return "ResultContainerWrapper [resultContainer=" + resultContainer + "]";
	}
	
}

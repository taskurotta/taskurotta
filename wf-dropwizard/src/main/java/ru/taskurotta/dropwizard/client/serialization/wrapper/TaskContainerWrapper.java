package ru.taskurotta.dropwizard.client.serialization.wrapper;

import ru.taskurotta.dropwizard.client.serialization.TaskContainerDeserializer;
import ru.taskurotta.backend.storage.model.TaskContainer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class TaskContainerWrapper {
	
	private TaskContainer taskContainer;

	public TaskContainer getTaskContainer() {
		return taskContainer;
	}
	
	public TaskContainerWrapper(){}
	
	public TaskContainerWrapper(TaskContainer taskContainer){
		this.taskContainer = taskContainer;
	}
	
	@JsonDeserialize(using=TaskContainerDeserializer.class)
	public void setTaskContainer(TaskContainer taskContainer) {
		this.taskContainer = taskContainer;
	}

	@Override
	public String toString() {
		return "TaskContainerWrapper [taskContainer=" + taskContainer + "]";
	}
	
	
}

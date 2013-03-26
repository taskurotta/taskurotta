package ru.taskurotta.client.serialization.wrapper;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ru.taskurotta.client.serialization.TaskContainerDeserializer;
import ru.taskurotta.server.transport.TaskContainer;

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

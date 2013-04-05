package ru.taskurotta.backend.storage.model;

import java.util.UUID;

public class TaskDefinition {
	
	private UUID taskId;
	private String actorId;
	private long startTime;
	private String taskList;
	private long executionStarted;
	
	public TaskDefinition(UUID taskId, String actorId, long startTime,
			String taskList, long executionStarted) {
		this.taskId = taskId;
		this.actorId = actorId;
		this.startTime = startTime;
		this.taskList = taskList;
		this.executionStarted = executionStarted;
	}

	public long getExecutionStarted() {
		return executionStarted;
	}

	public UUID getTaskId() {
		return taskId;
	}

	public String getActorId() {
		return actorId;
	}

	public long getStartTime() {
		return startTime;
	}

	public String getTaskList() {
		return taskList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actorId == null) ? 0 : actorId.hashCode());
		result = prime * result
				+ (int) (executionStarted ^ (executionStarted >>> 32));
		result = prime * result + (int) (startTime ^ (startTime >>> 32));
		result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
		result = prime * result
				+ ((taskList == null) ? 0 : taskList.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskDefinition other = (TaskDefinition) obj;
		if (actorId == null) {
			if (other.actorId != null)
				return false;
		} else if (!actorId.equals(other.actorId))
			return false;
		if (executionStarted != other.executionStarted)
			return false;
		if (startTime != other.startTime)
			return false;
		if (taskId == null) {
			if (other.taskId != null)
				return false;
		} else if (!taskId.equals(other.taskId))
			return false;
		if (taskList == null) {
			if (other.taskList != null)
				return false;
		} else if (!taskList.equals(other.taskList))
			return false;
		return true;
	}
	
}

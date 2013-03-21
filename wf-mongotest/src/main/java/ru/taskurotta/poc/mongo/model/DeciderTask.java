package ru.taskurotta.poc.mongo.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DeciderTask {
	private String _id;
	private int taskId;
	private int deciderId;
	private int processId;
	private List<Parameter> parameters = new ArrayList<Parameter>(5);
	private Parameter result;

	public DeciderTask() {
	}

	public DeciderTask(int taskId) {
		this.taskId = taskId;
		deciderId = 0;
	}

	public String get_id() {
		return _id;
	}

	public int getTaskId() {
		return taskId;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	public int getDeciderId() {
		return deciderId;
	}

	public void setDeciderId(int deciderId) {
		this.deciderId = deciderId;
	}

	public int getProcessId() {
		return processId;
	}

	public void setProcessId(int processId) {
		this.processId = processId;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public Parameter getResult() {
		return result;
	}

	public void setResult(Parameter result) {
		this.result = result;
	}
}

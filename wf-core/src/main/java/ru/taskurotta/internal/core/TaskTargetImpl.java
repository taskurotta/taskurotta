package ru.taskurotta.internal.core;

import ru.taskurotta.core.TaskTarget;
import ru.taskurotta.core.TaskType;

/**
 * User: jedy
 * Date: 26.12.12 16:35
 */
public class TaskTargetImpl implements TaskTarget {

	private TaskType type;
	private String name;
	private String version;
	private String method;

	public TaskTargetImpl(TaskType type, String name, String version, String method) {
		this.type = type;
		this.name = name;
		this.version = version;
		this.method = method;
	}

	public TaskType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getMethod() {
		return method;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TaskTargetImpl)) return false;

		TaskTargetImpl that = (TaskTargetImpl) o;

		if (!method.equals(that.method)) return false;
		if (!name.equals(that.name)) return false;
		if (type != that.type) return false;
		if (!version.equals(that.version)) return false;

		return true;
	}


	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + version.hashCode();
		result = 31 * result + method.hashCode();
		return result;
	}

    @Override
    public String toString() {
        return "TaskTargetImpl{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}


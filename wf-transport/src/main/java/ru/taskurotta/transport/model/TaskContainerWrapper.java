package ru.taskurotta.transport.model;

public class TaskContainerWrapper {

    private TaskContainer taskContainer;

    public TaskContainerWrapper() {
    }

    public TaskContainerWrapper(TaskContainer taskContainer) {
        this.taskContainer = taskContainer;
    }

    public TaskContainer getTaskContainer() {
        return taskContainer;
    }

    public void setTaskContainer(TaskContainer taskContainer) {
        this.taskContainer = taskContainer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskContainerWrapper that = (TaskContainerWrapper) o;

        if (taskContainer != null ? !taskContainer.equals(that.taskContainer) : that.taskContainer != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return taskContainer != null ? taskContainer.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TaskContainerWrapper{" +
                "taskContainer=" + taskContainer +
                "} " + super.toString();
    }
}

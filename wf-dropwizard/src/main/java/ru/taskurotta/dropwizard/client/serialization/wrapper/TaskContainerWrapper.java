package ru.taskurotta.dropwizard.client.serialization.wrapper;

import ru.taskurotta.backend.storage.model.TaskContainer;

public class TaskContainerWrapper {

    private TaskContainer taskContainer;

    public TaskContainer getTaskContainer() {
        return taskContainer;
    }

    public TaskContainerWrapper(){}

    public TaskContainerWrapper(TaskContainer taskContainer){
        this.taskContainer = taskContainer;
    }

    public void setTaskContainer(TaskContainer taskContainer) {
        this.taskContainer = taskContainer;
    }

    @Override
    public String toString() {
        return "TaskContainerWrapper [taskContainer=" + taskContainer + "]";
    }


}

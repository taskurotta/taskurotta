package ru.taskurotta.util;

import ru.taskurotta.backend.storage.model.TaskContainer;

/**
 * Configuration of a task to start.
 * User: dimadin
 * Date: 16.05.13 10:31
 */
public class SubmitterConfig {

    //Number of task to submit
    private int count = 1;

    //Endpoint of REST service to submit to
    private String endpoint;

    //TaskContainer description. ActorId and method are required, any other value would override default
    private TaskContainer task;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public TaskContainer getTask() {
        return task;
    }

    public void setTask(TaskContainer task) {
        this.task = task;
    }

}

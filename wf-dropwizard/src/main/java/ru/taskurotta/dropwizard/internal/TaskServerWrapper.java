package ru.taskurotta.dropwizard.internal;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.util.ActorDefinition;

public class TaskServerWrapper implements TaskServer {

    private static final Logger logger = LoggerFactory.getLogger(TaskServerWrapper.class);

    private TaskServer taskServer;

    private Map<String, Runnable> daemonTasks;

    @PostConstruct
    public void init() {
        if(daemonTasks!=null && !daemonTasks.isEmpty()) {
            for(String daemonName: daemonTasks.keySet()) {
                Thread runner = new Thread(daemonTasks.get(daemonName));
                runner.setDaemon(true);
                runner.setName(daemonName);
                runner.start();
            }
            logger.info("Started [{}] daemon tasks: [{}]", daemonTasks.size(), daemonTasks.keySet());
        }
    }

    @Override
    public void startProcess(TaskContainer task) {
        taskServer.startProcess(task);
    }
    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        return taskServer.poll(actorDefinition);
    }
    @Override
    public void release(DecisionContainer taskResult) {
        taskServer.release(taskResult);
    }

    public void setDaemonTasks(Map<String, Runnable> daemonTasks) {
        this.daemonTasks = daemonTasks;
    }

    @Required
    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

}

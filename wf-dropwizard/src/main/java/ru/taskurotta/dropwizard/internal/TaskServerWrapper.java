package ru.taskurotta.dropwizard.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.recovery.schedule.SimpleScheduler;
import ru.taskurotta.util.ActorDefinition;

import javax.annotation.PostConstruct;
import java.util.Map;

public class TaskServerWrapper implements TaskServer {

    private static final Logger logger = LoggerFactory.getLogger(TaskServerWrapper.class);

    private TaskServer taskServer;

    private Map<Runnable, String> daemonTasks;

    @PostConstruct
    public void init() {
        if(daemonTasks!=null && !daemonTasks.isEmpty()) {
            for(Runnable daemon: daemonTasks.keySet()) {
                SimpleScheduler scheduler = new SimpleScheduler();
                scheduler.setScheduledProcess(daemon);
                scheduler.setName(daemon.getClass().getName());
                scheduler.setSchedule(daemonTasks.get(daemon));
                scheduler.start();
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

    public void setDaemonTasks(Map<Runnable, String> daemonTasks) {
        this.daemonTasks = daemonTasks;
    }

    @Required
    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

}

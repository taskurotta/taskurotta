package ru.taskurotta.dropwizard.health;

import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.util.ActorDefinition;

public class CheckTaskServer extends HealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(CheckTaskServer.class);

    private TaskServer taskServer;

    @Override
    protected Result check() throws Exception {
        try {
            //TODO: add some real check server logic here
            taskServer.poll(ActorDefinition.valueOf("testme", "testme"));
            return Result.healthy();
        } catch(Exception e) {
            logger.error("CheckTaskServer failed!", e);
            return Result.unhealthy(e);
        }

    }

    @Required
    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

}

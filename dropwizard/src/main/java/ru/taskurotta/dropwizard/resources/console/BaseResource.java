package ru.taskurotta.dropwizard.resources.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.manager.ConsoleManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 21.05.13 12:46
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BaseResource {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected ConsoleManager consoleManager;

    public void setConsoleManager(ConsoleManager consoleManager) {
        this.consoleManager = consoleManager;
    }

}

package ru.taskurotta.dropwizard.resources.console.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.dropwizard.resources.console.schedule.model.TaskCommand;
import ru.taskurotta.dropwizard.resources.console.util.TaskContainerUtils;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.service.console.manager.ConsoleManager;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.AbortProcessOperation;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * Created on 21.04.2015.
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/operation/process")
public class ProcessOperationsResource {

    private static final Logger logger = LoggerFactory.getLogger(ProcessOperationsResource.class);

    private ConsoleManager consoleManager;
    private TaskServer taskServer;
    private OperationExecutor abortProcessOperationExecutor;

    private ObjectMapper mapper = new ObjectMapper();

    @POST
    @Path("/clone")
    public String cloneProcess(String processId) {
        String result = null;
        try {
            Process process = consoleManager.getProcess(UUID.fromString(processId));
            if (process != null && process.getStartTask() != null) {
                TaskContainer sTask = process.getStartTask();
                UUID newProcessId = UUID.randomUUID();
                taskServer.startProcess(new TaskContainer(newProcessId, newProcessId, null, sTask.getMethod(),
                        sTask.getActorId(), sTask.getType(), sTask.getStartTime(), 0, sTask.getArgs(), sTask.getOptions(),
                        sTask.isUnsafe(), sTask.getFailTypes()));
                result = mapper.writeValueAsString(newProcessId.toString());
                logger.debug("Started new process with id[{}] (cloned from [{}])", result, processId);
            }
        } catch (Exception e) {
            logger.error("Cannot clone processId["+processId+"]");
            throw new WebApplicationException(e);
        }

        return result;
    }

    @POST
    @Path("/create")
    public String createProcess(TaskCommand taskCommand) {
        String result = null;
        try {
            TaskContainer tc = TaskContainerUtils.createTask(taskCommand, -1);
            logger.debug("Try to create process with task [{}]", tc);

            taskServer.startProcess(tc);

            result = mapper.writeValueAsString(tc.getProcessId().toString());
        } catch (Exception e) {
            logger.error("Cannot create process with command["+taskCommand+"]", e);
        }

        return result;
    }

    @POST
    @Path("/abort")
    public void abortProcess(String processId) {
        abortProcessOperationExecutor.enqueue(new AbortProcessOperation(UUID.fromString(processId)));
    }

    @Required
    public void setConsoleManager(ConsoleManager consoleManager) {
        this.consoleManager = consoleManager;
    }

    @Required
    public void setTaskServer(TaskServer taskServer) {
        this.taskServer = taskServer;
    }

    @Required
    public void setAbortProcessOperationExecutor(OperationExecutor abortProcessOperationExecutor) {
        this.abortProcessOperationExecutor = abortProcessOperationExecutor;
    }
}

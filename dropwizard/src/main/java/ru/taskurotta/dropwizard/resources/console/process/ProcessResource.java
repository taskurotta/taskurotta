package ru.taskurotta.dropwizard.resources.console.process;

import com.google.common.base.Optional;
import ru.taskurotta.dropwizard.resources.console.BaseResource;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

/**
 * Resource providing data on processes for console.
 * list/search/card of processes
 * Date: 24.05.13 11:54
 */
@Path("/console/processes")
public class ProcessResource extends BaseResource {

    private static int DEFAULT_START_PAGE = 1;
    private static int DEFAULT_PAGE_SIZE = 10;
    private static int DEFAULT_STATUS = -1;

    @GET
    @Path("/search")
    public GenericPage<Process> findProcesses(@QueryParam("pageNum") Optional<Integer> pageNum,
                                       @QueryParam("pageSize") Optional<Integer> pageSize,
                                       @QueryParam("processId") Optional<String> processId,
                                       @QueryParam("customId") Optional<String> customId) {
        try {
            ProcessSearchCommand command = new ProcessSearchCommand();
            command.setPageNum(pageNum.or(DEFAULT_START_PAGE));
            command.setPageSize(pageSize.or(DEFAULT_PAGE_SIZE));
            command.setProcessId(processId.orNull());
            command.setCustomId(customId.orNull());
            command.setState(-1);
            GenericPage<Process> processes = consoleManager.findProcesses(command);
            logger.debug("Processes found are [{}]", processes);
            return processes;

        } catch (Throwable e) {
            logger.error("Error at getting processes list", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    @Path("/process/{processId}")
    public Process getProcess(@PathParam("processId") String processId) {

        try {
            Process process = consoleManager.getProcess(UUID.fromString(processId));
            if (process != null && process.getStartTask() == null) {//Stark task is required for process card
                process.setStartTask(consoleManager.getTask(process.getStartTaskId(), process.getProcessId()));
            }
            logger.debug("Process got by id[{}] is [{}]", processId, process);
            return process;

        } catch (Throwable e) {
            logger.error("Error at getting process by id["+processId+"]", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    public GenericPage<Process> listProcesses(@QueryParam("pageNum") Optional<Integer> pageNum,
                                              @QueryParam("pageSize") Optional<Integer> pageSize,
                                              @QueryParam("status") Optional<Integer> status,
                                              @QueryParam("type") Optional<String> type) {

        try {
            ProcessSearchCommand command = new ProcessSearchCommand();
            command.setPageNum(pageNum.or(DEFAULT_START_PAGE));
            command.setPageSize(pageSize.or(DEFAULT_PAGE_SIZE));
            command.setState(status.or(DEFAULT_STATUS));
            command.setActorId(type.orNull());
            GenericPage<Process> result = consoleManager.findProcesses(command);
            logger.debug("Processes page is [{}]. Command [{}]", result, command);

            return result;

        } catch (Throwable e) {
            logger.error("Error at getting processes list", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    @Path("/finished/count")
    public Integer getFinishedCount(@QueryParam("customId") Optional<String> customIdOpt) {
        String customId = customIdOpt.orNull();
        try {
            return consoleManager.getFinishedCount(customId);
        } catch(Throwable e) {
            logger.error("Error at getting finished processes count", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }


}

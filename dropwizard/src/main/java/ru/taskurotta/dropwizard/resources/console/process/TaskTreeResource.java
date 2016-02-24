package ru.taskurotta.dropwizard.resources.console.process;

import ru.taskurotta.dropwizard.resources.console.BaseResource;
import ru.taskurotta.service.console.model.TaskTreeVO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Date: 31.05.13 16:52
 */
@Path("/console/tree")
public class TaskTreeResource extends BaseResource {

    @GET
    @Path("/task/{processId}/{taskId}")
    public TaskTreeVO getTaskTree(@PathParam("type") String type, @PathParam("processId") String processId,
                                  @PathParam("taskId") String taskId) {

        try {
            TaskTreeVO result = consoleManager.getTreeForTask(UUID.fromString(taskId), UUID.fromString(processId));
            logger.debug("TaskTree for task id[{}], processId[{}] is [{}]", type, taskId, processId, result);
            return result;

        } catch (Throwable e) {
            logger.error("Error at getting task tree by taskId[" + taskId + "], processId[" +
                    processId + "] type[" + type + "]", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }


    @GET
    @Path("/process/{processId}/{taskId}")
    public TaskTreeVO getProcessTree(@PathParam("type") String type, @PathParam("processId") String processId,
                                     @PathParam("taskId") String taskId) {

        try {
            TaskTreeVO result = consoleManager.getTreeForProcess(UUID.fromString(processId));
            logger.debug("Got TaskTree by type[{}], id[{}], processId[{}] is [{}]", type, taskId, processId, result);
            return result;

        } catch (Throwable e) {
            logger.error("Error at getting task tree by taskId[" + taskId + "], processId[" +
                    processId + "] type[" + type + "]", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

    }

}

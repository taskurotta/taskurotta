package ru.taskurotta.dropwizard.resources.console;

import ru.taskurotta.backend.console.model.TaskTreeVO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 31.05.13 16:52
 */
@Path("/console/tree/{type}/{processId}/{taskId}")
public class TaskTreeResource extends BaseResource {

    private static final String TYPE_TASK = "task";
    private static final String TYPE_PROCESS = "process";

    @GET
    public Response getTaskTree(@PathParam("type")String type, @PathParam("processId")String processId, @PathParam("taskId")String taskId) {

        try {
            TaskTreeVO result = null;
            if(TYPE_TASK.equalsIgnoreCase(type)) {
                result = consoleManager.getTreeForTask(UUID.fromString(taskId), UUID.fromString(processId));
            } else if(TYPE_PROCESS.equalsIgnoreCase(type)) {
                result = consoleManager.getTreeForProcess(UUID.fromString(processId));
            }
            logger.debug("TaskTree getted by type[{}], id[{}], processId[{}] is [{}]", type, taskId, processId, result);
            return Response.ok(result, MediaType.APPLICATION_JSON).build();
        } catch(Throwable e) {
            logger.error("Error at getting task tree by taskId["+taskId+"], processId["+processId+"] type["+type+"]", e);
            return Response.serverError().build();
        }

    }

}

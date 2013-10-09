package ru.taskurotta.dropwizard.resources.console.process;

import ru.taskurotta.dropwizard.resources.console.BaseResource;
import ru.taskurotta.transport.model.DecisionContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 09.07.13 17:51
 */
@Path("/console/task/decision/{processId}/{taskId}")
public class TaskDecisionResource extends BaseResource {

    @GET
    public Response getTaskDecision(@PathParam("processId")String processId, @PathParam("taskId")String taskId) {

        try {
            DecisionContainer result = consoleManager.getDecision(UUID.fromString(taskId), UUID.fromString(processId));
            logger.debug("DecisionContainer getted by taskId[{}], processId[{}] is [{}]", taskId, processId, result);
            return Response.ok(result, MediaType.APPLICATION_JSON).build();
        } catch(Throwable e) {
            logger.error("Error at getting task decision by taskId["+taskId+"], processId["+processId+"]", e);
            return Response.serverError().build();
        }

    }

}

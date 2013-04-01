package ru.taskurotta.dropwizard.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.dropwizard.client.serialization.wrapper.DecisionContainerWrapper;
import ru.taskurotta.server.TaskServer;

import com.yammer.metrics.annotation.Timed;

@Path("/tasks/release")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskReleaserResource {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskReleaserResource.class);	
	private TaskServer taskServer;

	@POST
	@Timed
	public Response releaseAction(DecisionContainerWrapper resultContainer) {
		logger.debug("releaseAction resource called with entity[{}]", resultContainer);
		
		try {
			taskServer.release(resultContainer.getResultContainer());	
		} catch(Exception e) {
			logger.error("Starting of task["+resultContainer+"] failed!", e);
			return Response.serverError().build();
		}
		
		return Response.ok().build();
		
	}

	public void setTaskServer(TaskServer taskServer) {
		this.taskServer = taskServer;
	}
	
	
}

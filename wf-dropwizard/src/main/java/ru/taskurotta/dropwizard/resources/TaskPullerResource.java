package ru.taskurotta.dropwizard.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.dropwizard.client.serialization.wrapper.ActorDefinitionWrapper;
import ru.taskurotta.dropwizard.client.serialization.wrapper.TaskContainerWrapper;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.transport.TaskContainer;

import com.yammer.metrics.annotation.Timed;

@Path("/tasks/pull")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskPullerResource {
	private static final Logger logger = LoggerFactory.getLogger(TaskPullerResource.class);	
	private TaskServer taskServer;

	@POST
	@Timed
	public TaskContainerWrapper pullAction(ActorDefinitionWrapper actorDefinitionWrapper) throws Exception {
		logger.debug("pullAction called with entity[{}]", actorDefinitionWrapper);
		
		TaskContainer result = null;
		
		try {
			result = taskServer.pull(actorDefinitionWrapper.getActorDefinition());
			logger.debug("Task getted is[{}]", result);
		} catch(Exception e) {
			logger.error("Pulling task for["+actorDefinitionWrapper+"] failed!", e);
			throw e;
		}
		
		return new TaskContainerWrapper(result);
		
	}

	public void setTaskServer(TaskServer taskServer) {
		this.taskServer = taskServer;
	}
	
	

}

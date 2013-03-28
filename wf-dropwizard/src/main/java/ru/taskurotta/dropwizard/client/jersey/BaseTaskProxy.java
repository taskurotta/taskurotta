package ru.taskurotta.dropwizard.client.jersey;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.dropwizard.client.serialization.wrapper.ActorDefinitionWrapper;
import ru.taskurotta.dropwizard.client.serialization.wrapper.DecisionContainerWrapper;
import ru.taskurotta.dropwizard.client.serialization.wrapper.TaskContainerWrapper;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

public class BaseTaskProxy implements TaskServer {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String PULLER_RESOURCE = "/tasks/pull";
	public static final String START_RESOURCE = "/tasks/start";
	public static final String RELEASER_RESOURCE = "/tasks/release";

	protected Integer threadPoolSize = 0;//0 = new thread per request || thread pool size
	protected Integer connectTimeout = 0;//0 = infinite || value in ms
	protected Integer readTimeout = 0;//0 = infinite|| value in ms

	protected String endpoint = null;

	protected WebResource startResource;
	protected WebResource pullResource;
	protected WebResource releaseResource;

	@Override
	public void startProcess(TaskContainer task) {
		try {
			startResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(new TaskContainerWrapper(task));
		} catch (Throwable ex) {
			if (isReadTimeout(ex)) {
				logger.debug("Read timeout at start process for task[" + task + "]", ex);
			} else {
				logger.error("Unexpected error at start task[" + task + "]", ex);
			}
			throw new RuntimeException(ex);

		}

	}

	@Override
	public TaskContainer pull(ActorDefinition actorDefinition) {
		TaskContainer result = null;
		try {
			result = pullResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.post(TaskContainerWrapper.class, new ActorDefinitionWrapper(actorDefinition)).getTaskContainer();
		} catch (Throwable ex) {
			if (isReadTimeout(ex)) {
				logger.debug("Read timeout pulling task for [{}]", actorDefinition);
				//Just return null as if no task getted
			} else {
				logger.error("Unexpected error at pull task[" + actorDefinition + "] ", ex);
				throw new RuntimeException(ex);
			}


		}
		return result;
	}

	@Override
	public void release(DecisionContainer taskResult) {
		try {
			releaseResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(new DecisionContainerWrapper(taskResult));
		} catch (Throwable ex) {
			if (isReadTimeout(ex)) {
				logger.debug("Read timeout releasing [{}]", taskResult);
			} else {
				logger.error("Unexpected error at releasing task[" + taskResult + "]", ex);
				throw new RuntimeException(ex);
			}
		}
	}

	protected String getContextUrl(String path) {
		return endpoint.replaceAll("/*$", "") + "/" + path.replaceAll("^/*", "");
	}

	public void setThreadPoolSize(Integer threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public void setConnectTimeout(Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
	}

	@Required
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public boolean isReadTimeout(Throwable ex) {
		return java.net.SocketTimeoutException.class.isAssignableFrom(ex.getClass())
				|| (ex.getCause() != null && java.net.SocketTimeoutException.class.isAssignableFrom(ex.getCause().getClass()));
	}

}

package ru.taskurotta.client.jersey;

import java.util.Map;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.client.serialization.wrapper.ActorDefinitionWrapper;
import ru.taskurotta.client.serialization.wrapper.ResultContainerWrapper;
import ru.taskurotta.client.serialization.wrapper.TaskContainerWrapper;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.server.transport.DecisionContainer;
import ru.taskurotta.server.transport.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import javax.ws.rs.core.MediaType;

public class JerseyTaskServerProxy implements TaskServer {
	
	public static final String PULLER_RESOURCE = "/tasks/pull";
	public static final String START_RESOURCE = "/tasks/start";
	public static final String RELEASER_RESOURCE = "/tasks/release";
	
	private static final Logger logger = LoggerFactory.getLogger(JerseyTaskServerProxy.class);
	
	private String endpoint = null;
	
	private Client client = null;
	
	private WebResource startResource;
	private WebResource pullResource;
	private WebResource releaseResource;

	public JerseyTaskServerProxy(String endpoint){
		this(endpoint, null);
	}
	
	public JerseyTaskServerProxy(String endpoint, Map<String, Object> properties) {
		this.endpoint = endpoint;
		ClientConfig cc = new DefaultClientConfig();
		if(properties!=null && !properties.isEmpty()) {
			cc.getProperties().putAll(properties);
		}
		client = Client.create(cc);
		startResource = client.resource(getContextUrl(START_RESOURCE));
		pullResource = client.resource(getContextUrl(PULLER_RESOURCE));
		releaseResource = client.resource(getContextUrl(RELEASER_RESOURCE));
		//client.addFilter(new LoggingFilter(System.out));
	}
	
	@Override
	public void startProcess(TaskContainer task) {
		//long start = System.currentTimeMillis();
		
		logger.debug("Client: startProcess([{}])", task);
		startResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
			.post(new TaskContainerWrapper(task));
		
		//logger.info("start task in [{}]ms", (System.currentTimeMillis() - start));
	}

	@Override
	public TaskContainer pull(ActorDefinition actorDefinition) {
		//long start = System.currentTimeMillis();
		//logger.debug("Client: pull([{}])", actorDefinition);
		TaskContainer result =  pullResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.post(TaskContainerWrapper.class, new ActorDefinitionWrapper(actorDefinition)).getTaskContainer();
		
		//logger.debug("Task getted is [{}]", result);
		
		//logger.info("pull in [{}]ms", (System.currentTimeMillis() - start));
		return result;
	}

	@Override
	public void release(DecisionContainer taskResult) {
		long start = System.currentTimeMillis();
		//logger.debug("Client: release([{}])", taskResult);
		releaseResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
			.post(new ResultContainerWrapper(taskResult));
		
		//logger.info("[{}]: release in [{}]ms", new Date().getTime(),(System.currentTimeMillis() - start));
	}
	
	private String getContextUrl(String path) {
		return endpoint.replaceAll("/*$", "") + "/" + path.replaceAll("^/*", "");
	}	

	

}

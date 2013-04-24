package ru.taskurotta.dropwizard.client.jersey;

import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.dropwizard.client.serialization.wrapper.ActorDefinitionWrapper;
import ru.taskurotta.dropwizard.client.serialization.wrapper.DecisionContainerWrapper;
import ru.taskurotta.dropwizard.client.serialization.wrapper.TaskContainerWrapper;
import ru.taskurotta.exception.Retriable;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.util.ActorDefinition;

import javax.ws.rs.core.MediaType;

public class BaseTaskProxy implements TaskServer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PULLER_RESOURCE = "/tasks/poll";
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
            WebResource.Builder rb = startResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            rb.post(new TaskContainerWrapper(task));
        } catch(Throwable ex) {
            if(isReadTimeout(ex)) {
                logger.debug("Read timeout at start process for task[" + task + "]", ex);//TODO: or error level here?
                throw new Retriable("Process start failed, retry operation required... Task["+task+"]");
            } else {
                logger.error("Unexpected error at start task["+task+"]", ex);
                throw new RuntimeException(ex);
            }
        }

    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        TaskContainer result = null;
        try {
            WebResource.Builder rb = pullResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            result =  rb.post(TaskContainerWrapper.class, new ActorDefinitionWrapper(actorDefinition)).getTaskContainer();
        } catch(Throwable ex) {
            if(isReadTimeout(ex)) {
                logger.debug("Read timeout polling task for [{}]", actorDefinition);
                //Just return null as if no task getted
            } else {
                logger.error("Unexpected error at poll task["+actorDefinition+"] ", ex);
                throw new RuntimeException(ex);
            }


        }
        return result;
    }

    @Override
    public void release(DecisionContainer taskResult) {
        try {
            WebResource.Builder rb = releaseResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            rb.post(new DecisionContainerWrapper(taskResult));
        } catch(Throwable ex) {
            if(isReadTimeout(ex)) {
                logger.debug("Read timeout releasing [{}]", taskResult);
                //TODO: just return and rely on recovery? Or try to release again?
            } else {
                logger.error("Unexpected error at releasing task["+taskResult+"]", ex);
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

    //Returns true if exception or any of it's nested causes is a java.net.SocketTimeoutException
    public boolean isReadTimeout(Throwable ex) {
        boolean result = false;
        if(ex!=null) {
            result = java.net.SocketTimeoutException.class.isAssignableFrom(ex.getClass())
                    || isReadTimeout(ex.getCause());
        }
        return result;
    }

}

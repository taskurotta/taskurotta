package ru.taskurotta.dropwizard.client.jersey;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.storage.model.DecisionContainer;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.dropwizard.client.serialization.wrapper.ActorDefinitionWrapper;
import ru.taskurotta.dropwizard.client.serialization.wrapper.DecisionContainerWrapper;
import ru.taskurotta.dropwizard.client.serialization.wrapper.TaskContainerWrapper;
import ru.taskurotta.exception.server.InvalidServerRequestException;
import ru.taskurotta.exception.server.ServerConnectionException;
import ru.taskurotta.exception.server.ServerException;
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
        } catch(UniformInterfaceException ex) {//server responded with error
            int status = ex.getResponse()!=null? ex.getResponse().getStatus(): -1;
            logger.error("Server responded with ["+status+"] error at start process with task["+task+"]", ex);
            if(status>=400 && status<500) {
                throw new InvalidServerRequestException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error", ex);
            } else {
                throw new ServerException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error", ex);
            }
        } catch(ClientHandlerException ex) {//client processing error
            logger.error("Client failed to process request for task["+task+"]", ex);
            throw new ServerConnectionException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error", ex);
        } catch(Throwable ex) {//unexpected error
            logger.error("Unexpected error at start process for task ["+task+"]", task);
            throw new ServerException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error", ex);
        }

    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        logger.trace("Polling task thread name[{}]", Thread.currentThread().getName());
        TaskContainer result = null;
        try {
            WebResource.Builder rb = pullResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            result =  rb.post(TaskContainerWrapper.class, new ActorDefinitionWrapper(actorDefinition)).getTaskContainer();
        } catch(UniformInterfaceException ex) {//server responded with error
            int status = ex.getResponse()!=null? ex.getResponse().getStatus(): -1;
            logger.error("Server responded with ["+(ex.getResponse()!=null? ex.getResponse().getStatus():"no response code")+"] error at polling task for["+actorDefinition+"]", ex);
            if(status>=400 && status<500) {
                throw new InvalidServerRequestException("Poll error for actor["+actorDefinition+"]", ex);
            } else {
                throw new ServerException("Poll error for actor["+actorDefinition+"]", ex);
            }
        } catch(ClientHandlerException ex) {//client processing error
            logger.error("Client failed to process poll request for actorDefinition["+actorDefinition+"]", ex);
            throw new ServerConnectionException("Poll error for actor["+actorDefinition+"]", ex);
        } catch(Throwable ex) {//unexpected error
            logger.error("Unexpected error at poll request for actorDefinition["+actorDefinition+"]", ex);
            throw new ServerException("Poll error for actor["+actorDefinition+"]", ex);
        }
        return result;
    }

    @Override
    public void release(DecisionContainer taskResult) {
        logger.trace("Releasing task thread name[{}]", Thread.currentThread().getName());
        try {
            WebResource.Builder rb = releaseResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            rb.post(new DecisionContainerWrapper(taskResult));
        } catch(UniformInterfaceException ex) {//server responded with error
            int status = ex.getResponse()!=null? ex.getResponse().getStatus(): -1;
            logger.error("Server responded with ["+(ex.getResponse()!=null? ex.getResponse().getStatus():"no response code")+"] error at releasing taskResult["+taskResult+"]", ex);
            if(status>=400 && status<500) {
                throw new InvalidServerRequestException("Task release ["+taskResult.getTaskId()+"] error", ex);
            } else{
                throw new ServerException("Task release ["+taskResult.getTaskId()+"] error", ex);
            }
        } catch(ClientHandlerException ex) {//client processing error
            logger.error("Client failed to process release request for taskresult["+taskResult+"]", ex);
            throw new ServerConnectionException("Task release ["+taskResult.getTaskId()+"] error", ex);
        } catch(Throwable ex) {//unexpected error
            logger.error("Unexpected error at release request for taskResult["+taskResult+"]", ex);
            throw new ServerException("Task release ["+taskResult.getTaskId()+"] error", ex);
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

}

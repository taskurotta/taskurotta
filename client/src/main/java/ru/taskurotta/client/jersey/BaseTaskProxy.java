package ru.taskurotta.client.jersey;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.server.InvalidServerRequestException;
import ru.taskurotta.exception.server.ServerConnectionException;
import ru.taskurotta.exception.server.ServerException;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.util.ActorDefinition;

import javax.ws.rs.core.MediaType;

public class BaseTaskProxy implements TaskServer {

    public static final String REST_SERVICE_PREFIX = "/rest/";
    protected final Logger logger = LoggerFactory.getLogger(getClass());

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
            rb.post(task);
        } catch(UniformInterfaceException ex) {//server responded with error
            int status = ex.getResponse()!=null? ex.getResponse().getStatus(): -1;
            if (status>=400 && status<500) {
                throw new InvalidServerRequestException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error: " + ex.getMessage(), ex);
            } else {
                throw new ServerException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error: " + ex.getMessage(), ex);
            }
        } catch(ClientHandlerException ex) {//client processing error
            throw new ServerConnectionException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error: " + ex.getMessage(), ex);
        } catch(Throwable ex) {//unexpected error
            throw new ServerException("Start process["+task.getProcessId()+"] with task["+task.getTaskId()+"] error: " + ex.getMessage(), ex);
        }

    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {
        logger.trace("Polling task thread name[{}]", Thread.currentThread().getName());
        TaskContainer result;
        try {
            WebResource.Builder rb = pullResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            result = rb.post(TaskContainer.class, actorDefinition);
        } catch(UniformInterfaceException ex) {//server responded with error
            int status = ex.getResponse()!=null? ex.getResponse().getStatus(): -1;
            if (status>=400 && status<500) {
                throw new InvalidServerRequestException("Poll error for actor["+actorDefinition+"]: " + ex.getMessage(), ex);
            } else {
                throw new ServerException("Poll error for actor["+actorDefinition+"]: " + ex.getMessage(), ex);
            }
        } catch(ClientHandlerException ex) {//client processing error
            throw new ServerConnectionException("Poll error for actor["+actorDefinition+"]: " + ex.getMessage(), ex);
        } catch(Throwable ex) {//unexpected error
            throw new ServerException("Poll error for actor["+actorDefinition+"]: " + ex.getMessage(), ex);
        }
        return result;
    }

    @Override
    public void release(DecisionContainer taskResult) {
        logger.trace("Releasing task thread name[{}], taskResult[{}]", Thread.currentThread().getName(), taskResult);
        try {
            WebResource.Builder rb = releaseResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            rb.post(taskResult);
        } catch(UniformInterfaceException ex) {//server responded with error
            int status = ex.getResponse()!=null? ex.getResponse().getStatus(): -1;
            if (status>=400 && status<500) {
                throw new InvalidServerRequestException("Task release ["+taskResult.getTaskId()+"] error: " + ex.getMessage(), ex);
            } else{
                throw new ServerException("Task release ["+taskResult.getTaskId()+"] error: " + ex.getMessage(), ex);
            }
        } catch(ClientHandlerException ex) {//client processing error
            throw new ServerConnectionException("Task release ["+taskResult.getTaskId()+"] error: " + ex.getMessage(), ex);
        } catch(Throwable ex) {//unexpected error
            throw new ServerException("Task release ["+taskResult.getTaskId()+"] error: " + ex.getMessage(), ex);
        }
    }

    protected String getContextUrl(String path) {
        return endpoint.replaceAll("/*$", "") + REST_SERVICE_PREFIX + path.replaceAll("^/*", "");
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

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}

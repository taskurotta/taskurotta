package ru.taskurotta.client.jersey;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.server.InvalidServerRequestException;
import ru.taskurotta.exception.server.ServerException;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.UpdateTimeoutRequest;
import ru.taskurotta.util.ActorDefinition;

import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BaseTaskProxy implements TaskServer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected long threadPoolSize = 0;//0 = new thread per request || thread pool size
    protected long connectTimeout = 0;//0 = infinite || value in ms
    protected long readTimeout = 0;//0 = infinite|| value in ms

    protected String endpoint = null;

    ConcurrentHashMap<String, ServerResources> servers = new ConcurrentHashMap<String, ServerResources>();
    ConcurrentHashMap<ActorDefinition, String> queueOwnersMap = new ConcurrentHashMap<ActorDefinition, String>();

    @Override
    public void startProcess(TaskContainer task) {

        if (logger.isTraceEnabled()) {
            logger.trace("Start process thread name[{}]", Thread.currentThread().getName());
        }

        WebResource startResource = servers.get(endpoint).getStartResource();

        try {
            WebResource.Builder rb = startResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            rb.post(task);
        } catch (Throwable ex) {

            if (ex instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) ex;
            }

            String msg = createStartProcessErrorMessage(startResource.getURI(), task.getActorId(),
                    task.getProcessId(), ex);

            checkAndThrowInvalidServerRequestException(msg, ex);
        }
    }

    private ServerResources getQueueOwnerServerResource(ActorDefinition actorDefinition) {
        String ownerEndpoint = queueOwnersMap.get(actorDefinition);

        if (ownerEndpoint != null) {
            return getServerResource(ownerEndpoint);
        }

        return servers.get(endpoint);
    }

    private ServerResources getServerResource(String ownerEndpoint) {

        if (ownerEndpoint == null) {
            return servers.get(endpoint);
        }

        ServerResources serverResources = servers.get(ownerEndpoint);

        if (serverResources != null) {
            return serverResources;
        }

        serverResources = servers.get(endpoint).createNewServerResource(ownerEndpoint);
        servers.putIfAbsent(ownerEndpoint, serverResources);

        return serverResources;
    }

    private void removeQueueOwner(ActorDefinition actorDefinition) {
        queueOwnersMap.remove(actorDefinition);
    }

    @Override
    public TaskContainer poll(ActorDefinition actorDefinition) {

        if (logger.isTraceEnabled()) {
            logger.trace("Polling task thread name[{}]", Thread.currentThread().getName());
        }

        WebResource pollResource = getQueueOwnerServerResource(actorDefinition).getPollResource();

        if (logger.isDebugEnabled()) {
            logger.error("poll actor [{}] tasks from uri [{}]", actorDefinition, pollResource.getURI());
        }

        try {
            WebResource.Builder rb = pollResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON, MediaType.MEDIA_TYPE_WILDCARD);

            ClientResponse clientResponse = rb.post(ClientResponse.class, actorDefinition);

            if (clientResponse.getStatus() != 204) {//204 = no-content

                String queueOwner = null;
                List<String> queueOwnerList = clientResponse.getHeaders().get(TaskServer.FIELD_QUEUE_OWNER);
                if (queueOwnerList != null && !queueOwnerList.isEmpty()) {
                    queueOwner = queueOwnerList.get(0);
                }

                TaskServer.queueOwner.remove();
                if (queueOwner != null) {
                    logger.debug("queue owner of actor [{}] is [{}]", actorDefinition, queueOwner);
                    queueOwnersMap.put(actorDefinition, queueOwner);
                }

                String processOwner = null;
                List<String> processOwnerList = clientResponse.getHeaders().get(TaskServer.FIELD_PROCESS_OWNER);
                if (processOwnerList != null && !processOwnerList.isEmpty()) {
                    processOwner = processOwnerList.get(0);
                }

                TaskServer.processOwner.remove();
                if (processOwner != null) {
                    logger.debug("task process owner of actor [{}] is [{}]", actorDefinition, processOwner);
                    TaskServer.processOwner.set(processOwner);
                }

                return clientResponse.getEntity(TaskContainer.class);
            }
        } catch (Throwable ex) {

            removeQueueOwner(actorDefinition);

            if (ex instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) ex;
            }

            String msg = createPollErrorMessage(pollResource.getURI(), actorDefinition, ex);

            checkAndThrowInvalidServerRequestException(msg, ex);
        }
        return null;
    }


    @Override
    public void release(DecisionContainer taskResult) {

        if (logger.isTraceEnabled()) {
            logger.trace("Releasing task thread name[{}], taskResult[{}]", Thread.currentThread().getName(),
                    taskResult);
        }

        WebResource releaseResource = getServerResource(TaskServer.processOwner.get()).getReleaseResource();

        if (logger.isDebugEnabled()) {
            logger.debug("release task [{}] to process owner [{}]", taskResult.getProcessId(), releaseResource.getURI());
        }

        try {
            WebResource.Builder rb = releaseResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            rb.post(taskResult);
        } catch (Throwable ex) {

            if (ex instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) ex;
            }

            String msg = createReleaseErrorMessage(releaseResource.getURI(), taskResult.getActorId(),
                    taskResult.getTaskId(), ex);

            checkAndThrowInvalidServerRequestException(msg, ex);
        }
    }

    @Override
    public void updateTaskTimeout(UUID taskId, UUID processId, long timeout) {

        UpdateTimeoutRequest updateTimeoutRequest = new UpdateTimeoutRequest(taskId, processId, timeout);

        if (logger.isTraceEnabled()) {
            logger.trace("Update task timeout thread name[{}], request[{}]",
                    Thread.currentThread().getName(), updateTimeoutRequest);
        }

        WebResource updateTimeoutResource = servers.get(endpoint).getUpdateTimeoutResource();

        try {
            WebResource.Builder rb = updateTimeoutResource.getRequestBuilder();
            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON);
            rb.post(updateTimeoutRequest);
        } catch (Throwable ex) {

            if (ex instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) ex;
            }

            String msg = createUpdateTimeoutErrorMessage(updateTimeoutResource.getURI(), null, taskId, ex);

            checkAndThrowInvalidServerRequestException(msg, ex);
        }

    }


    private static void checkAndThrowInvalidServerRequestException(String msg, Throwable ex) {

        if (ex instanceof UniformInterfaceException) {
            ClientResponse response = ((UniformInterfaceException) ex).getResponse();
            int status = response != null ? response.getStatus() : -1;
            if (status >= 400 && status < 500) {
                throw new InvalidServerRequestException(msg, ex);
            }
        }

        throw new ServerException(msg, ex);
    }

    private String createUpdateTimeoutErrorMessage(URI uri, String actorId, UUID taskId, Throwable ex) {
        return createErrorMessage("Update task timeout error [" + taskId.toString() + "]", uri, actorId, ex);
    }

    private String createReleaseErrorMessage(URI uri, String actorId, UUID taskId, Throwable ex) {
        return createErrorMessage("Release task error [" + taskId.toString() + "]", uri, actorId, ex);
    }

    private String createStartProcessErrorMessage(URI uri, String actorId, UUID processId, Throwable ex) {
        return createErrorMessage("Start process error [" + processId + "]", uri, actorId, ex);
    }

    private String createPollErrorMessage(URI uri, ActorDefinition actorDefinition, Throwable ex) {
        return createErrorMessage("Poll error", uri, actorDefinition.toString(), ex);
    }

    private String createErrorMessage(String msg, URI uri, String actorId, Throwable ex) {
        String ip = null;
        String host = uri.getHost();
        InetAddress address = null;
        try {
            address = InetAddress.getByName(host);
            if (address == null) {
                ip = "unknown ip";
            } else {
                ip = address.getHostAddress();
            }
        } catch (Throwable e) {
            ip = e.getMessage();
        }

        return msg + " actor[" + actorId + "], URI [" + uri.toString() + "], host & ip ["
                + host + "/" + ip + "]: " + ex.getMessage();
    }

    public void setThreadPoolSize(long threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}

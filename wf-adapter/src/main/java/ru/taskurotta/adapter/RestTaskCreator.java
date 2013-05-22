package ru.taskurotta.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.adapter.json.ObjectFactory;
import ru.taskurotta.transport.model.ActorSchedulingOptionsContainer;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskContainerWrapper;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.model.TaskType;

import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 22.05.13
 * Time: 11:54
 */
public class RestTaskCreator implements TaskCreator {

    private static final Logger logger = LoggerFactory.getLogger(RestTaskCreator.class);

    private String taskServerEndpoint;
    private ObjectFactory objectFactory = new ObjectFactory();

    public RestTaskCreator(String taskServerEndpoint) {
        this.taskServerEndpoint = taskServerEndpoint;
    }

    @Override
    public String createTask(String actorId, String method, Object[] args, String customId, long startTime, String taskList) {
        logger.trace("Try to create TaskContainer from arguments actorId=[{}], method=[{}], args=[{}], customId=[{}], startTime=[{}], taskList=[{}]", actorId, method, args, customId, startTime, taskList);

        UUID taskId = UUID.randomUUID();
        UUID processId = UUID.randomUUID();

        ArgContainer[] argContainers = null;
        if (args != null) {
            int argLength = args.length;
            argContainers = new ArgContainer[argLength];
            for (int i = 0; i < argLength; i++) {
                argContainers[i] = objectFactory.dumpArg(args[i]);
            }
        }

        ActorSchedulingOptionsContainer actorSchedulingOptionsContainer = new ActorSchedulingOptionsContainer();
        actorSchedulingOptionsContainer.setCustomId(customId);
        actorSchedulingOptionsContainer.setStartTime(startTime);
        actorSchedulingOptionsContainer.setTaskList(taskList);

        TaskOptionsContainer taskOptionsContainer = new TaskOptionsContainer(null, actorSchedulingOptionsContainer, null);

        TaskContainer taskContainer = new TaskContainer(taskId, processId, method, actorId, TaskType.DECIDER_START, startTime, 1, argContainers, taskOptionsContainer);
        logger.debug("Create task container [{}] from from arguments actorId=[{}], method=[{}], args=[{}], customId=[{}], startTime=[{}], taskList=[{}]",
                        taskContainer, actorId, method, args, customId, startTime, taskList);

        sendTask(taskContainer);

        return taskId.toString();
    }

    private void sendTask(TaskContainer taskContainer) {
        logger.trace("Try to send task container [{}] to task server by endpoint [{}]", taskContainer, taskServerEndpoint);

        try {
            Client client = Client.create();
            WebResource webResource = client.resource(getContextUrl(taskServerEndpoint, START_RESOURCE));
            WebResource.Builder requestBuilder = webResource.getRequestBuilder();
            requestBuilder.type(MediaType.APPLICATION_JSON);
            requestBuilder.accept(MediaType.APPLICATION_JSON);

            String jsonValue = objectFactory.getMapper().writeValueAsString(new TaskContainerWrapper(taskContainer));
            logger.debug("Send JSON object [{}] to task server by endpoint [{}]", jsonValue, taskServerEndpoint);

            requestBuilder.post(jsonValue);
        } catch (ClientHandlerException e) {
            logger.error("Can't send task [" + taskContainer + "] to task server by endpoint [" + taskServerEndpoint + "]");
            throw new RuntimeException("Can't send task [" + taskContainer + "] to task server by endpoint [" + taskServerEndpoint + "]", e);
        } catch(UniformInterfaceException ex) {
            int status = ex.getResponse() != null ? ex.getResponse().getStatus() : -1;
            logger.error("Start process [" + taskContainer.getProcessId() + "] with task [" + taskContainer.getTaskId() + "] answer status [" + status + "] error: " + ex.getMessage());
            throw new RuntimeException("Start process [" + taskContainer.getProcessId() + "] with task [" + taskContainer.getTaskId() + "] answer status [" + status + "] error: " + ex.getMessage(), ex);
        } catch (JsonProcessingException e) {
            logger.error("Can't convert task [" + taskContainer + "] to JSON object");
            throw new RuntimeException("Can't convert task [" + taskContainer + "] to JSON object", e);
        }
    }

    private static String getContextUrl(String endpoint, String path) {
        return endpoint.replaceAll("/*$", "") + "/" + path.replaceAll("^/*", "");
    }
}

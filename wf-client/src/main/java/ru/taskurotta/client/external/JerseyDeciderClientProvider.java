package ru.taskurotta.client.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.ProxyFactory;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.core.Task;
import ru.taskurotta.exception.server.ServerConnectionException;
import ru.taskurotta.internal.RuntimeContext;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskContainerWrapper;

import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 22.05.13
 * Time: 16:22
 */
public class JerseyDeciderClientProvider implements DeciderClientProvider {

    private static final Logger logger = LoggerFactory.getLogger(JerseyDeciderClientProvider.class);

    public static final String START_RESOURCE = "/tasks/start";

    private String taskServerEndpoint;
    private ObjectFactory objectFactory;

    public JerseyDeciderClientProvider(String taskServerEndpoint) {
        this.taskServerEndpoint = taskServerEndpoint;
        this.objectFactory = new ObjectFactory();
    }

    @Override
    public <DeciderClientType> DeciderClientType getDeciderClient(Class<DeciderClientType> type) {
        return ProxyFactory.getDeciderClient(type, new RuntimeContext(null) {
            @Override
            public void handle(Task task) {
                startProcess(task);
            }

            /**
             * Always creates new process uuid for new tasks because each DeciderClientType invocation are start of new
             * process.
             *
             * @return process UUID
             */
            public UUID getProcessId() {
                return UUID.randomUUID();
            }
        });
    }

    @Override
    public void startProcess(Task task) {
        logger.trace("Try to start process with task [{}]", task);

        TaskContainer taskContainer = objectFactory.dumpTask(task);
        logger.debug("Create task container [{}] from task [{}]", taskContainer, task);

        String resource = getContextUrl(taskServerEndpoint, START_RESOURCE);
        try {
            Client client = Client.create();
            WebResource webResource = client.resource(resource);
            WebResource.Builder requestBuilder = webResource.getRequestBuilder();
            requestBuilder.type(MediaType.APPLICATION_JSON);
            requestBuilder.accept(MediaType.APPLICATION_JSON);

            String jsonValue = objectFactory.writeAsString(new TaskContainerWrapper(taskContainer));
            requestBuilder.post(jsonValue);
            logger.debug("Send JSON [{}] to task server to resource [{}]", jsonValue, resource);
        } catch (UniformInterfaceException e) {
            int status = e.getResponse() !=null ? e.getResponse().getStatus() : -1;
            logger.error("While send task container [" + taskContainer + "] to task server resource [" + resource + "] got status [" + status + "]", e);
            throw new RuntimeException("While send task container [" + taskContainer + "] to task server resource [" + resource + "] got status [" + status + "]", e);
        } catch(ClientHandlerException e) {
            logger.error("Start process[" + taskContainer.getProcessId() + "] with task [" + taskContainer.getTaskId() + "] error: " + e.getMessage(), e);
            throw new ServerConnectionException("Start process[ " + taskContainer.getProcessId() + "] with task [" + taskContainer.getTaskId() + "] error: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            logger.error("Error while convert task container [" + taskContainer + "] to JSON", e);
            throw new RuntimeException("Error while convert task container [" + taskContainer + "] to JSON", e);
        }
    }

    private static String getContextUrl(String endpoint, String path) {
        return endpoint.replaceAll("/*$", "") + "/" + path.replaceAll("^/*", "");
    }
}
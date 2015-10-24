package ru.taskurotta.test.stress;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import ru.taskurotta.dropwizard.resources.monitoring.StatMonitorResource;
import ru.taskurotta.exception.server.ServerException;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import javax.ws.rs.core.MediaType;

/**
 */
public class HttpFinishedProcessCounter implements ProcessesCounter {

    protected WebResource finishedProcessCounterResource;

    public HttpFinishedProcessCounter(String endpoint) {

        SimpleHttpConnectionManager connectionManager = new SimpleHttpConnectionManager();

        HttpClient httpClient = new HttpClient(connectionManager);
        ApacheHttpClientHandler httpClientHandler = new ApacheHttpClientHandler(httpClient);
        ApacheHttpClient contentServerClient = new ApacheHttpClient(httpClientHandler);

        finishedProcessCounterResource = contentServerClient.resource(TransportUtils.getRestPath(endpoint,
                StatMonitorResource.PATH + StatMonitorResource.METHOD_FINISHED_PROCESS_COUNTER));
    }

    @Override
    public int getCount() {

        TaskContainer result = null;
        try {
            WebResource.Builder rb = finishedProcessCounterResource.getRequestBuilder();

            rb.type(MediaType.APPLICATION_JSON);
            rb.accept(MediaType.APPLICATION_JSON, MediaType.MEDIA_TYPE_WILDCARD);

            return Integer.valueOf(rb.get(String.class));
        } catch (UniformInterfaceException | ClientHandlerException ex) {//server responded with error
                throw new ServerException("Can not receive finished process counter due rest API: " + ex.getMessage(),
                        ex);
        }
    }
}

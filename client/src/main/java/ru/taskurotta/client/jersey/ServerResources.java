package ru.taskurotta.client.jersey;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import ru.taskurotta.server.TaskServerResource;
import ru.taskurotta.transport.utils.TransportUtils;

/**
 */
class ServerResources {

    private WebResource startResource;
    private WebResource pollResource;
    private WebResource releaseResource;
    private WebResource updateTimeoutResource;

    String endpoint;
    long threadPoolSize;
    long connectTimeout;
    long readTimeout;
    int maxConnectionsPerHost;

    ServerResources(String endpoint, long threadPoolSize, long connectTimeout,
                    long readTimeout, int maxConnectionsPerHost) {

        this.endpoint = endpoint;
        this.threadPoolSize = threadPoolSize;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.maxConnectionsPerHost = maxConnectionsPerHost;

        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

        if (connectTimeout > 0) {
            connectionManager.getParams().setConnectionTimeout((int) connectTimeout);
        }
        if (readTimeout > 0) {
            connectionManager.getParams().setSoTimeout((int) readTimeout);
        }
        if (threadPoolSize > 0) {
            connectionManager.getParams().setMaxTotalConnections((int) threadPoolSize);
        }
        if (maxConnectionsPerHost > 0) {
            connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
        }


        HttpClient httpClient = new HttpClient(connectionManager);

        ApacheHttpClientHandler httpClientHandler = new ApacheHttpClientHandler(httpClient);
        ApacheHttpClient contentServerClient = new ApacheHttpClient(httpClientHandler);
        contentServerClient.setConnectTimeout((int) connectTimeout);
        contentServerClient.setReadTimeout((int) readTimeout);

        startResource = contentServerClient.resource(TransportUtils.getRestPath(endpoint, TaskServerResource.START));
        pollResource = contentServerClient.resource(TransportUtils.getRestPath(endpoint, TaskServerResource.POLL));
        releaseResource = contentServerClient.resource(TransportUtils.getRestPath(endpoint, TaskServerResource.RELEASE));
        updateTimeoutResource = contentServerClient.resource(TransportUtils.getRestPath(endpoint,
                TaskServerResource.UPDATE_TIMEOUT));
    }

    public WebResource getStartResource() {
        return startResource;
    }

    public WebResource getPollResource() {
        return pollResource;
    }

    public WebResource getReleaseResource() {
        return releaseResource;
    }

    public WebResource getUpdateTimeoutResource() {
        return updateTimeoutResource;
    }


    public ServerResources createNewServerResource(String newEndpoint) {
        return new ServerResources("http://" + newEndpoint + ":8811", threadPoolSize, connectTimeout, readTimeout, maxConnectionsPerHost);
    }
}

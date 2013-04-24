package ru.taskurotta.dropwizard.client.jersey;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import javax.annotation.PostConstruct;

/**
 * TaskServer implementation as a simple native jersey client proxy.
 */
public class JerseyTaskServerProxy extends BaseTaskProxy {

    @PostConstruct
    public void init() {
        ClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, threadPoolSize);
        cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
        cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);

        Client client = Client.create(cc);

        startResource = client.resource(getContextUrl(START_RESOURCE));
        pullResource = client.resource(getContextUrl(PULLER_RESOURCE));
        releaseResource = client.resource(getContextUrl(RELEASER_RESOURCE));

        //Prints JSON request to console
        //client.addFilter(new LoggingFilter(System.out));

    }

}

package ru.taskurotta.client.jersey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * TaskServer implementation as a jersey client proxy. Uses embedded apache HTTP client for connection pooling
 */
public class JerseyHttpTaskServerProxy extends BaseTaskProxy {

    private static final Logger logger = LoggerFactory.getLogger(JerseyHttpTaskServerProxy.class);

    private int maxConnectionsPerHost;

    @PostConstruct
    public void init() {

        servers.put(endpoint,
                new ServerResources(endpoint, threadPoolSize, connectTimeout, readTimeout, maxConnectionsPerHost));

    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }
}
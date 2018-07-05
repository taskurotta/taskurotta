package ru.taskurotta.client;

import ru.taskurotta.client.internal.CommonClientServiceManager;
import ru.taskurotta.client.jersey.JerseyHttpTaskServerProxy;
import ru.taskurotta.server.json.ObjectFactory;

/**
 */
public class Taskurotta {

    DeciderClientProvider clientProvider;

    public Taskurotta(String endpoint) {
        this(endpoint, 1, 3000, 0);
    }

    public Taskurotta(String endpoint, int threadPoolSize, long connectionTimeout, long readTimeout) {

        JerseyHttpTaskServerProxy taskServer = new JerseyHttpTaskServerProxy();
        taskServer.setConnectTimeout(connectionTimeout);
        taskServer.setReadTimeout(readTimeout);
        taskServer.setThreadPoolSize(threadPoolSize);
        taskServer.setEndpoint(endpoint);
        taskServer.setMaxConnectionsPerHost(threadPoolSize);
        taskServer.init();

        ClientServiceManager clientServiceManager = new CommonClientServiceManager(taskServer, new ObjectFactory());

        this.clientProvider = clientServiceManager.getDeciderClientProvider();
    }

    public <T> T createDecider(Class<T> clazz) {
        return clientProvider.getDeciderClient(clazz);
    }
}

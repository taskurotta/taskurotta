package ru.taskurotta.test.stress.process;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.Taskurotta;
import ru.taskurotta.client.internal.CommonClientServiceManager;
import ru.taskurotta.client.jersey.JerseyHttpTaskServerProxy;
import ru.taskurotta.core.TaskConfig;
import ru.taskurotta.server.json.ObjectFactory;
import ru.taskurotta.test.fullfeature.decider.FullFeatureDeciderClient;

/**
 */
public class FullFeatureStarter implements Starter {

    public static FullFeatureDeciderClient deciderClient;


    public FullFeatureStarter(ClientServiceManager clientServiceManager) {

        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        deciderClient = clientProvider.getDeciderClient(FullFeatureDeciderClient.class);
    }

    @Override
    public void start(String customId) {
        deciderClient.start(new TaskConfig().setCustomId(customId));
    }


    /**
     * Example of programmatic process start
     */
    public static void mainOldStyle(String[] args) {

        JerseyHttpTaskServerProxy taskServer = new JerseyHttpTaskServerProxy();
        taskServer.setConnectTimeout(3000);
        taskServer.setReadTimeout(0);
        taskServer.setThreadPoolSize(10);
        taskServer.setEndpoint("http://tsk_http:80");
        taskServer.setMaxConnectionsPerHost(10);
        taskServer.init();

        ClientServiceManager clientServiceManager = new CommonClientServiceManager(taskServer, new ObjectFactory());

        DeciderClientProvider clientProvider = clientServiceManager.getDeciderClientProvider();
        FullFeatureDeciderClient decider = clientProvider.getDeciderClient(FullFeatureDeciderClient.class);

        decider.start(new TaskConfig().setCustomId("xxx"));
    }

    /**
     * Example of programmatic process start
     */
    public static void main(String[] args) {

        Taskurotta taskurotta = new Taskurotta("http://tsk_http:80");
        FullFeatureDeciderClient decider = taskurotta.createDecider(FullFeatureDeciderClient.class);

        decider.start(new TaskConfig().setCustomId("xxx"));
    }

}

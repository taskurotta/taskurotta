package ru.taskurotta.client.jersey;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Required;

import com.sun.jersey.api.client.config.ClientConfig;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.client.internal.DeciderClientProviderCommon;
import ru.taskurotta.client.internal.TaskSpreaderProviderCommon;
import ru.taskurotta.server.TaskServer;

public class JerseyClientServiceManager implements ClientServiceManager {

    private TaskServer taskServer;
    
    private String endpoint;
    
	private Integer threadPoolSize = 0;//0 = new thread per request || thread pool size
    private Integer connectTimeout = 0;//0 = infinite || value in ms
    private Integer readTimeout = 0;//0 = infinite|| value in ms
    
    @Override
    public DeciderClientProvider getDeciderClientProvider() {
        return new DeciderClientProviderCommon(getTaskServer());
    }

    @Override
    public TaskSpreaderProvider getTaskSpreaderProvider() {
        return new TaskSpreaderProviderCommon(getTaskServer());
    }

	public TaskServer getTaskServer() {
		if(taskServer == null) {
			Map<String, Object> props = new HashMap<String, Object>();
			props.put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeout);
			props.put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
			props.put(ClientConfig.PROPERTY_THREADPOOL_SIZE, threadPoolSize);
			taskServer = new JerseyTaskServerProxy(endpoint, props);
		}
		
		return taskServer;
	}
	
	@Required
    public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public void setTaskServer(TaskServer taskServer) {
		this.taskServer = taskServer;
	}

	public void setConnectTimeout(Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
	}
	
	public void setThreadPoolSize(Integer threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

}

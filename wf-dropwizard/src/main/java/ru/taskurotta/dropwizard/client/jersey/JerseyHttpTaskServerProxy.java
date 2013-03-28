package ru.taskurotta.dropwizard.client.jersey;

import javax.annotation.PostConstruct;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;

public class JerseyHttpTaskServerProxy extends BaseTaskProxy {
	
	private int maxConnectionsPerHost;
	
	@PostConstruct
	public void init() {
		MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		
		if(connectTimeout>0) {
			connectionManager.getParams().setConnectionTimeout(connectTimeout);	
		}
		if(readTimeout>0) {
			connectionManager.getParams().setSoTimeout(readTimeout);	
		}
		if(threadPoolSize>0) {
			connectionManager.getParams().setMaxTotalConnections(threadPoolSize);	
		}
		if(maxConnectionsPerHost>0) {
			connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);	
		}
		
		
		HttpClient httpClient = new HttpClient(connectionManager);

		ApacheHttpClientHandler httpClientHandler = new ApacheHttpClientHandler(httpClient);
		ApacheHttpClient contentServerClient = new ApacheHttpClient(httpClientHandler){
			
		};
		contentServerClient.setConnectTimeout(connectTimeout);
		contentServerClient.setReadTimeout(readTimeout);

		startResource = contentServerClient.resource(getContextUrl(START_RESOURCE));
		pullResource = contentServerClient.resource(getContextUrl(PULLER_RESOURCE));
		releaseResource = contentServerClient.resource(getContextUrl(RELEASER_RESOURCE));
		
	}

	public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
		this.maxConnectionsPerHost = maxConnectionsPerHost;
	}
	
}

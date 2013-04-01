package ru.taskurotta.example.calculate;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.DeciderClientProvider;
import ru.taskurotta.example.calculate.decider.MathActionDeciderClient;

public class WorkflowStarter {
	
	private ClientServiceManager clientServiceManager;
	
	private int count;
	
	private static final Logger logger = LoggerFactory.getLogger(WorkflowStarter.class);
	
	public void startWork() {
        DeciderClientProvider deciderClientProvider = clientServiceManager.getDeciderClientProvider();
        MathActionDeciderClient decider = deciderClientProvider.getDeciderClient(MathActionDeciderClient.class);
        
        
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SS");
		logger.info("Start work time [{}], count[{}]", sdf.format(new Date()), count);        
        for (int i = 0; i < count; i++) {
        	decider.performAction();
        }
        
	}
		
	public void setClientServiceManager(ClientServiceManager clientServiceManager) {
		this.clientServiceManager = clientServiceManager;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
}

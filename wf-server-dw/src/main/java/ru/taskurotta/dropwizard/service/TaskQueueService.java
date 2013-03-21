package ru.taskurotta.dropwizard.service;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.metrics.core.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.taskurotta.dropwizard.TaskQueueConfig;

import javax.ws.rs.Path;
import java.util.Map;

public class TaskQueueService extends Service<TaskQueueConfig> {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskQueueService.class);
	
	@Override
	public void initialize(Bootstrap<TaskQueueConfig> bootstrap) {
		bootstrap.setName("task-queue-service");
	}

	@Override
	public void run(TaskQueueConfig configuration, Environment environment)
			throws Exception {
		String contextLocation = configuration.getContextLocation();
		ApplicationContext appContext = new ClassPathXmlApplicationContext(contextLocation);
		
		//Register resources
		Map<String, Object> resources = appContext.getBeansWithAnnotation(Path.class);
		if(resources!=null && !resources.isEmpty()) {
			for(String resourceBeanName: resources.keySet()) {
				Object resourceSingleton = appContext.getBean(resourceBeanName);
				environment.addResource(resourceSingleton);
			}
			logger.info("Registered[{}] resources from application context location [{}]", resources.size(), contextLocation);			
		} else {
			//No resources - no fun
			logger.error("Application context [{}] contains no beans annotated with [{}]", contextLocation, Path.class.getName());
			throw new IllegalStateException("No resources found in context["+contextLocation+"]");
		}
		
		//Register healthchecks
		Map<String, HealthCheck> healthChecks = appContext.getBeansOfType(HealthCheck.class);
		if(healthChecks!=null && !healthChecks.isEmpty()) {
			for(String hcBeanName: healthChecks.keySet()) {
				HealthCheck healthCheck = appContext.getBean(hcBeanName, HealthCheck.class);
				environment.addHealthCheck(healthCheck);
			}
			logger.info("Registered[{}] healchChecks from application context location [{}]", healthChecks.size(), contextLocation);
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		new TaskQueueService().run(args);
	}
	
}

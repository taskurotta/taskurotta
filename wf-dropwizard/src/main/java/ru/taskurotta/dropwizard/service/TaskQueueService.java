package ru.taskurotta.dropwizard.service;

import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import ru.taskurotta.backend.config.impl.ConfigBackendAware;
import ru.taskurotta.dropwizard.TaskQueueConfig;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.metrics.core.HealthCheck;

public class TaskQueueService extends Service<TaskQueueConfig> {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskQueueService.class);
	
	@Override
	public void initialize(Bootstrap<TaskQueueConfig> bootstrap) {
		bootstrap.setName("task-queue-service");
	}

	@Override
	public void run(final TaskQueueConfig configuration, Environment environment)
			throws Exception {
		
		logger.debug("YAML config custom properties getted[{}]", configuration.getProperties());
		
		String contextLocation = configuration.getContextLocation();
		AbstractApplicationContext appContext = new ClassPathXmlApplicationContext(new String[]{contextLocation}, false);
		if(configuration.getProperties()!=null && !configuration.getProperties().isEmpty()) {
			appContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("customProperties", configuration.getProperties()));
			if(configuration.getInternalPoolConfig()!=null) {
				Properties internalPoolProperties = configuration.getInternalPoolConfig().asProperties();
				logger.debug("YAML config internal pool properties getted[{}]", internalPoolProperties);
				appContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("internalPoolConfigProperties", internalPoolProperties));
			}
						
		}
		
		
		if(configuration.getActorConfig() != null) {
			appContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
					beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
						@Override
						public Object postProcessBeforeInitialization(Object bean, String beanName)
								throws BeansException {
							if(bean instanceof ConfigBackendAware) {
								((ConfigBackendAware)bean).setConfigBackend(configuration.getActorConfig());
							}
							return bean;
						}
						@Override
						public Object postProcessAfterInitialization(Object bean, String beanName)
								throws BeansException {
							return bean;
						}
					});
				}
			});			
		}
		appContext.refresh();
		
//		if(configuration.getServerConfig() != null) {
//			Map<String, ServerConfigAware> serverConfigAwareBeans = appContext.getBeansOfType(ServerConfigAware.class);
//			if(serverConfigAwareBeans!=null && !serverConfigAwareBeans.isEmpty()) {
//				for(ServerConfigAware sca: serverConfigAwareBeans.values()) {
//					sca.setServerConfig(configuration.getServerConfig());
//				}
//			}
//		}
		
		
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

package ru.taskurotta.console.internal.core;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.metrics.core.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import javax.ws.rs.Path;
import java.util.Map;

/**
 * Dropwizard service class exploiting Springframework as a resources lifecycle manager
 * User: dimadin
 * Date: 17.05.13 16:10
 */
public class ConsoleService extends Service<ConsoleConfig>  {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleService.class);

    @Override
    public void initialize(Bootstrap<ConsoleConfig> bootstrap) {
        bootstrap.setName("console-service");
        bootstrap.addBundle(new AssetsBundle("/assets/", "/console/"));//serving static resources for angularJS
    }

    @Override
    public void run(ConsoleConfig configuration, Environment environment)
            throws Exception {

        logger.debug("YAML config custom properties getted[{}]", configuration.getProperties());

        //Exposing yaml properties to spring context
        String contextLocation = configuration.getContextLocation();
        AbstractApplicationContext appContext = new ClassPathXmlApplicationContext(new String[]{contextLocation}, false);
        if(configuration.getProperties()!=null && !configuration.getProperties().isEmpty()) {
            appContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("customProperties", configuration.getProperties()));
        }

        appContext.refresh();

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
            logger.info("Registered[{}] healthChecks from application context location [{}]", healthChecks.size(), contextLocation);
        }


    }

    public static void main(String[] args) throws Exception {
        new ConsoleService().run(args);
    }

}

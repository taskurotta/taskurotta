package ru.taskurotta.dropwizard.resources.console.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.taskurotta.dropwizard.server.ServerPropertiesAware;
import ru.taskurotta.service.config.ConfigService;
import ru.taskurotta.service.dependency.DependencyService;
import ru.taskurotta.service.gc.GarbageCollectorService;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.storage.BrokenProcessService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.service.storage.TaskService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Javadoc should be here
 * Date: 19.12.13 11:15
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/context")
public class ContextResource implements ServerPropertiesAware, ApplicationContextAware {

    public static final Logger logger = LoggerFactory.getLogger(ContextResource.class);

    protected Properties properties;
    protected ApplicationContext applicationContext;


    private Map<String, String> configBeans;

    @GET
    @Path("/service")
    public ContextInfo getConfigurationInformation() {
        ContextInfo result = new ContextInfo();

        if (configBeans == null) {
            configBeans = getConfigBeansMap();
        }

        result.setProperties(getTaskServerProperties());
        result.setConfigBeans(configBeans);
        result.setStartupDate(applicationContext.getStartupDate());

        return result;
    }

    protected Map<String, String> getConfigBeansMap() {
        Map<String, String> result = new HashMap<>();

        appendBeansInfo(result, applicationContext.getBeanNamesForType(QueueService.class));
        appendBeansInfo(result, applicationContext.getBeanNamesForType(ProcessService.class));
        appendBeansInfo(result, applicationContext.getBeanNamesForType(TaskService.class));
        appendBeansInfo(result, applicationContext.getBeanNamesForType(DependencyService.class));
        appendBeansInfo(result, applicationContext.getBeanNamesForType(BrokenProcessService.class));
        appendBeansInfo(result, applicationContext.getBeanNamesForType(GarbageCollectorService.class));
        appendBeansInfo(result, applicationContext.getBeanNamesForType(ConfigService.class));

        logger.debug("Service beans are [{}]", result);
        return result;
    }

    protected void appendBeansInfo(Map<String, String> result, String[] beanNames) {
        if (beanNames!=null && beanNames.length>0 && result!=null) {
            StringBuilder sb = new StringBuilder();
            for (String beanName : beanNames) {
                Object bean = applicationContext.getBean(beanName);
                String className = null;

                if (AopUtils.isAopProxy(bean)) {
                    className = ((TargetClassAware)bean).getTargetClass().getName();
                } else {
                    className = bean!=null? bean.getClass().getName(): "";
                }

                result.put(beanName, className);
            }
        }
    }


    public static class ContextInfo implements Serializable {
        protected Map<String, String> configBeans;
        protected Map<String, String> properties;
        protected long startupDate;

        public Map<String, String> getConfigBeans() {
            return configBeans;
        }

        public void setConfigBeans(Map<String, String> configBeans) {
            this.configBeans = configBeans;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public long getStartupDate() {
            return startupDate;
        }

        public void setStartupDate(long startupDate) {
            this.startupDate = startupDate;
        }
    }

    @GET
    @Path("/props")
    //TODO: filter values with a special prefix/suffix? Exposing all props to the web is not a good idea
    public Map<String, String> getTaskServerProperties() {
        Map<String, String> result = null;
        if (properties!=null && !properties.isEmpty()) {
            result = new HashMap<>();
            for (String key : properties.stringPropertyNames()) {
                result.put(key, properties.getProperty(key, ""));
            }
        }
        return result;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}

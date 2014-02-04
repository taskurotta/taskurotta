package ru.taskurotta.spring.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.taskurotta.bootstrap.config.SpreaderConfig;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.util.ActorDefinition;

import java.util.Collection;
import java.util.Properties;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 17:54
 */
public class SpreaderConfigPathXmlApplicationContext implements SpreaderConfig {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigPathXmlApplicationContext.class);

    private AbstractApplicationContext applicationContext;
    private TaskSpreaderProvider taskSpreaderProvider;

    private String context;
    private String[] contexts;
    private Properties properties;

    @Override
    public void init() {
        if (applicationContext == null) {

            if (context != null) {
                applicationContext = new ClassPathXmlApplicationContext(new String[]{context}, false);
            } else {
                applicationContext = new ClassPathXmlApplicationContext(contexts, false);
            }

            if (properties != null && !properties.isEmpty()) {
                PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
                propertyPlaceholderConfigurer.setProperties(properties);

                applicationContext.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
            }

            applicationContext.refresh();
        }

        Class taskSpreaderProviderClass = TaskSpreaderProvider.class;

        try {
            try {
                Collection clientServiceManagerBeans = applicationContext.getBeansOfType(ClientServiceManager.class).values();
                ClientServiceManager clientServiceManager = (ClientServiceManager) clientServiceManagerBeans.iterator().next();
                taskSpreaderProvider = clientServiceManager.getTaskSpreaderProvider();
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug("Not found bean of [{}]", taskSpreaderProviderClass);
            }

//            if (taskSpreaderProvider == null) {
//                ClientServiceManager clientServiceManager = new ClientServiceManagerMemory();
//                taskSpreaderProvider = clientServiceManager.getTaskSpreaderProvider();
//            }
        } catch (BeansException e) {
            logger.error("Not found bean of class [{}]", taskSpreaderProviderClass);
            throw new RuntimeException("Not found bean of class", e);
        }
    }

    @Override
    public TaskSpreader getTaskSpreader(Class clazz) {
        return taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(clazz));
    }

    @Override
    public TaskSpreader getTaskSpreader(Class clazz, String taskList) {
        return taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(clazz, taskList));
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setContexts(String[] context) {
        this.contexts = contexts;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}

package ru.taskurotta.spring.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import ru.taskurotta.bootstrap.config.SpreaderConfig;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.client.memory.ClientServiceManagerMemory;
import ru.taskurotta.util.ActorDefinition;

import java.io.IOException;
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

    private String defaultPropertiesLocation;
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
                System.err.println("properties: " + properties);
                applicationContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("customProperties", properties));
            }

            if (defaultPropertiesLocation != null) {
                Properties defaultProperties = null;
                try {
                    defaultProperties = PropertiesLoaderUtils.loadAllProperties(defaultPropertiesLocation);
                } catch (IOException e) {
                    throw new IllegalStateException("Can not load default properties", e);
                }

                System.err.println("defaultProperties [" + defaultPropertiesLocation + "]: " + defaultProperties);
                applicationContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource
                        ("defaultProperties", defaultProperties));
            }

            applicationContext.refresh();
        }

        Class taskSpreaderProviderClass = TaskSpreaderProvider.class;

        try {
            try {
                ClientServiceManager clientServiceManager = applicationContext.getBean(ClientServiceManager.class);
                taskSpreaderProvider = clientServiceManager.getTaskSpreaderProvider();
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug("Not found bean of [{}]", taskSpreaderProviderClass);
            }

            if (taskSpreaderProvider == null) {
                ClientServiceManager clientServiceManager = new ClientServiceManagerMemory();
                taskSpreaderProvider = clientServiceManager.getTaskSpreaderProvider();
            }
        } catch (BeansException e) {
            logger.error("Not found bean of class [{}]", taskSpreaderProviderClass);
            throw new RuntimeException("Not found bean of class", e);
        }
    }

    @Override
    public TaskSpreader getTaskSpreader(Class clazz) {
        return taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(clazz));
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setContexts(String[] contexts) {
        this.contexts = contexts;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setDefaultPropertiesLocation(String defaultPropertiesLocation) {
        this.defaultPropertiesLocation = defaultPropertiesLocation;
    }
}

package ru.taskurotta.spring.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import ru.taskurotta.bootstrap.config.SpreaderConfig;
import ru.taskurotta.client.ClientServiceManager;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.client.TaskSpreaderProvider;
import ru.taskurotta.util.ActorDefinition;

import java.util.Properties;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 17:54
 */
public class SpreaderConfigPathXmlApplicationContext extends AbstractConfigClassPathXmlApplicationContext implements SpreaderConfig {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigPathXmlApplicationContext.class);

    private TaskSpreaderProvider taskSpreaderProvider;

    @Override
    public void init() {
        super.init();

        Class taskSpreaderProviderClass = TaskSpreaderProvider.class;

        try {
            try {
                ClientServiceManager clientServiceManager = applicationContext.getBean(ClientServiceManager.class);
                taskSpreaderProvider = clientServiceManager.getTaskSpreaderProvider();
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug("Not found bean of [{}]", taskSpreaderProviderClass);
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

    @Override
    public TaskSpreader getTaskSpreader(Class clazz, String taskList) {
        return taskSpreaderProvider.getTaskSpreader(ActorDefinition.valueOf(clazz, taskList));
    }

    public Properties getProperties() {
        return properties;
    }
}

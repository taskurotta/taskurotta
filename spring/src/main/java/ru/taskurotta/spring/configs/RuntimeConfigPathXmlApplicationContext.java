package ru.taskurotta.spring.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.RuntimeProviderManager;
import ru.taskurotta.bootstrap.config.RuntimeConfig;

import java.util.Properties;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 15:08
 */
public class RuntimeConfigPathXmlApplicationContext extends AbstractConfigClassPathXmlApplicationContext implements RuntimeConfig {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigPathXmlApplicationContext.class);

    private RuntimeProvider runtimeProvider;

    @Override
    public void init() {
        super.init();

        Class<RuntimeProvider> runtimeProviderClass = RuntimeProvider.class;

        try {
            try {
                runtimeProvider = applicationContext.getBean(runtimeProviderClass);
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug("Not found bean of [{}]", runtimeProviderClass);
            }

            if (runtimeProvider == null) {
                runtimeProvider = RuntimeProviderManager.getRuntimeProvider();
            }
        } catch (BeansException e) {
            logger.error("Not found bean of class [{}]", runtimeProviderClass);
            throw new RuntimeException("Not found bean of class", e);
        }
    }

    @Override
    public RuntimeProcessor getRuntimeProcessor(Class actorInterface) {
        Object bean = applicationContext.getBean(actorInterface);
        return runtimeProvider.getRuntimeProcessor(bean);
    }

    public Properties getProperties() {
        return this.properties;
    }
}

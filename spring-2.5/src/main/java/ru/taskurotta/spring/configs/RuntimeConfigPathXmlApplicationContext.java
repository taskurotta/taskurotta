package ru.taskurotta.spring.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.RuntimeProvider;
import ru.taskurotta.RuntimeProviderManager;
import ru.taskurotta.bootstrap.config.RuntimeConfig;

import java.util.Collection;
import java.util.Properties;

/**
 * User: stukushin
 * Date: 06.02.13
 * Time: 15:08
 */
public class RuntimeConfigPathXmlApplicationContext implements RuntimeConfig {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigPathXmlApplicationContext.class);

    private AbstractApplicationContext applicationContext;
    private RuntimeProvider runtimeProvider;

    private String context;
    private Properties properties;

    @Override
    public void init() {

        logger.debug("context [{}]", context);

        if (applicationContext == null) {
            applicationContext = new ClassPathXmlApplicationContext(new String[]{context}, false);

            if (properties != null && !properties.isEmpty()) {
                PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
                propertyPlaceholderConfigurer.setProperties(properties);

                applicationContext.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
            }

            applicationContext.refresh();
        }

        Class<RuntimeProvider> runtimeProviderClass = RuntimeProvider.class;

        try {
            try {
                Collection runtimeProviderCollection = applicationContext.getBeansOfType(runtimeProviderClass).values();
                runtimeProvider = (RuntimeProvider) runtimeProviderCollection.iterator().next();
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

        Collection actorInterfaceBeans = applicationContext.getBeansOfType(actorInterface).values();
        Object bean = actorInterfaceBeans.iterator().next();

        return runtimeProvider.getRuntimeProcessor(bean);
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

	public AbstractApplicationContext getApplicationContext() {
		return applicationContext;
	}
}

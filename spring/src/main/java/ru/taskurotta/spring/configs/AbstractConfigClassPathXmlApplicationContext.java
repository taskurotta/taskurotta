package ru.taskurotta.spring.configs;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * User: stukushin
 * Date: 06.11.2014
 * Time: 17:29
 */

abstract class AbstractConfigClassPathXmlApplicationContext {

    protected ClassPathXmlApplicationContext applicationContext;

    protected String context;
    protected String[] contexts;
    protected Properties properties;
    protected String defaultPropertiesLocation;

    public void init() {
        if (applicationContext == null) {

            if (context != null) {
                applicationContext = new ClassPathXmlApplicationContext(new String[]{context}, false);
            } else {
                applicationContext = new ClassPathXmlApplicationContext(contexts, false);
            }

            if (properties != null && !properties.isEmpty()) {
                applicationContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("customProperties", properties));
            }

            if (defaultPropertiesLocation != null) {
                Properties defaultProperties;
                try {
                    defaultProperties = PropertiesLoaderUtils.loadAllProperties(defaultPropertiesLocation);
                } catch (IOException e) {
                    throw new IllegalStateException("Can not load default properties", e);
                }

                applicationContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("defaultProperties", defaultProperties));
            }

            applicationContext.refresh();
        }
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

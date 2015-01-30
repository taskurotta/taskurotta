package ru.taskurotta.spring.configs;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import ru.taskurotta.util.MemoryAllocationConfigurator;
import ru.taskurotta.util.PropertiesUtil;

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

            Properties result = properties != null? properties: new Properties();

            if (defaultPropertiesLocation != null) {
                Properties defaultProperties;
                try {
                    defaultProperties = PropertiesLoaderUtils.loadAllProperties(defaultPropertiesLocation);
                } catch (IOException e) {
                    throw new IllegalStateException("Can not load default properties", e);
                }

                result = PropertiesUtil.addProperties(defaultProperties, result, null);
            }

            result = PropertiesUtil.addProperties(result, MemoryAllocationConfigurator.calculate(result), null);

            applicationContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource
                    ("customProperties", result));

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

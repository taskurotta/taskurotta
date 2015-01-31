package ru.taskurotta.spring.configs;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;
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

            if (StringUtils.hasText(defaultPropertiesLocation)) {
                Properties defaultProperties = new Properties();

                String[] locations = StringUtils.commaDelimitedListToStringArray(defaultPropertiesLocation);
                for (String location : locations) {
                    Properties props;
                    try {
                        props = PropertiesLoaderUtils.loadAllProperties(location.trim());
                    } catch (IOException e) {
                        throw new IllegalStateException("Can not load properties from [" + location + "]", e);
                    }
                    defaultProperties = PropertiesUtil.addProperties(defaultProperties, props);
                }

                result = PropertiesUtil.addProperties(defaultProperties, result);
            }

            result = PropertiesUtil.addProperties(result, MemoryAllocationConfigurator.calculate(result));
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

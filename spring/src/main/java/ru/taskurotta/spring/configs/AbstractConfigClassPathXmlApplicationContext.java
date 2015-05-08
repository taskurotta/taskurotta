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
    protected String[] defaultPropertiesLocations;

    public void init() {
        if (applicationContext == null) {

            if (context != null) {
                applicationContext = new ClassPathXmlApplicationContext(new String[]{context}, false);
            } else {
                applicationContext = new ClassPathXmlApplicationContext(contexts, false);
            }

            Properties result = new Properties();
            Properties traceSource = new Properties();

            if (StringUtils.hasText(defaultPropertiesLocation)) {
                Properties defaultProperties;
                try {
                    defaultProperties = PropertiesLoaderUtils.loadAllProperties(defaultPropertiesLocation);

                } catch (IOException e) {
                    throw new IllegalStateException("Can not load default properties", e);
                }

                result = PropertiesUtil.mergeProperties(result, defaultProperties, traceSource, "default");
            }

            if (defaultPropertiesLocations != null) {

                int i = 0;
                for (String location : defaultPropertiesLocations) {
                    Properties props;
                    try {
                        props = PropertiesLoaderUtils.loadAllProperties(location.trim());
                    } catch (IOException e) {
                        throw new IllegalStateException("Can not load properties from [" + location + "]", e);
                    }
                    result = PropertiesUtil.mergeProperties(result, props, traceSource, "default[" + ++i + "]");
                }
            }

            result = PropertiesUtil.mergeProperties(result, properties, traceSource, "yaml");

            result = PropertiesUtil.mergeProperties(result, MemoryAllocationConfigurator.calculate(result), traceSource,
                    "memory");

            result = PropertiesUtil.mergeProperties(result, System.getProperties(), traceSource, "system");

            PropertiesUtil.dumpProperties(this.getClass().getName(), result, traceSource);

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

    public void setDefaultPropertiesLocations(String defaultPropertiesLocations) {
        this.defaultPropertiesLocations = defaultPropertiesLocations.split(",");
    }
}
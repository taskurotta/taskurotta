package ru.taskurotta.bootstrap.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Created on 22.08.2014.
 */
public class PropertiesInjector {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesInjector.class);

    public static void injectProperties(JsonNode propertiesAwareNode, String propertiesLocation, String fieldName) {
        Properties props = getProperties(propertiesLocation);
        if (props != null) {
            injectProperties(propertiesAwareNode, props, fieldName);
        } else {
            logger.error("Cannot find properties by location [{}]", propertiesLocation);
        }
    }

    public static void injectProperties(JsonNode propertiesAwareNode, Properties properties, String fieldName) {
        if (properties != null) {
            ObjectNode propNode = (ObjectNode)propertiesAwareNode.with(fieldName);
            Enumeration<String> propNames = (Enumeration<String>) properties.propertyNames();
            while (propNames.hasMoreElements()) {
                String key = propNames.nextElement();
                propNode.put(key, properties.getProperty(key));
            }
        }
    }

    public static void injectSystemProperties(JsonNode propertiesAwareNode, String prefix, String fieldName) {
        injectProperties(propertiesAwareNode, getSystemPropertiesByPrefix(prefix), fieldName);
    }

    public static Properties getSystemPropertiesByPrefix(String prefix) {
        Properties result = new Properties();
        Properties sysProps = System.getProperties();
        Enumeration<String> sysPropsNames = (Enumeration<String>) sysProps.propertyNames();
        while (sysPropsNames.hasMoreElements()) {
            String key = sysPropsNames.nextElement();
            if (key.startsWith(prefix)) {
                result.setProperty(key.substring(prefix.length()+1), sysProps.getProperty(key));
            }
        }
        return result.size()>0? result: null;
    }

    public static Properties getProperties (String location) {
        Properties result = null;
        File locationFile = new File(location);
        InputStream is = null;
        try {
            if (locationFile.exists()) {
                is = new FileInputStream(locationFile);
            } else {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
            }

            if (is != null) {
                result = new Properties();
                result.load(is);
                is.close();
            }

        } catch(IOException e) {
            logger.error("Cannot parse properties for location["+location+"]", e);
        }

        return result;
    }

}

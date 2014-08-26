package ru.taskurotta.bootstrap.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.config.serialization.YamlConfigDeserializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

/**
 * Created on 22.08.2014.
 */
public class PropertiesInjector {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesInjector.class);

    public static void injectConfigurationProperties(JsonNode cfgNode, String propertiesLocation) {
        Properties props = getProperties(propertiesLocation);
        if (props != null) {
            injectInstanceProperties(cfgNode.get(YamlConfigDeserializer.YAML_RUNTIME), props);
            injectInstanceProperties(cfgNode.get(YamlConfigDeserializer.YAML_SPREADER), props);
            injectInstanceProperties(cfgNode.get(YamlConfigDeserializer.YAML_RPOFILER), props);
            injectInstanceProperties(cfgNode.get(YamlConfigDeserializer.YAML_POLICY), props);
            injectActorProperties(cfgNode.get(YamlConfigDeserializer.YAML_ACTOR), props);
        } else {
            logger.error("Cannot find properties by location [{}]", propertiesLocation);
        }
    }

    private static void injectActorProperties(JsonNode jsonNode, Properties props) {
        if (jsonNode != null) {
            for (Iterator nodeElements = jsonNode.elements(); nodeElements.hasNext(); ) {
                JsonNode element = (JsonNode) nodeElements.next();
                String actorName = element.fieldNames().next();
                JsonNode instanceNode = element.elements().next();
                injectPropertiesByPrefix(instanceNode, props, actorName);
                injectPropertiesAsNodeValues(instanceNode, getFilteredByPrefix(props, actorName + ".cfg."));
            }
        }
    }

    private static void injectPropertiesAsNodeValues(JsonNode objectNode, Properties props) {
        if (objectNode != null && props != null && objectNode instanceof ObjectNode) {
            Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();
            while (propNames.hasMoreElements()) {
                String key = propNames.nextElement();
                String value = props.getProperty(key);
                ((ObjectNode) objectNode).put(key, value);
            }
        }
    }

    private static void injectInstanceProperties(JsonNode jsonNode, Properties props) {
        if (jsonNode != null) {
            for (Iterator nodeElements = jsonNode.elements(); nodeElements.hasNext(); ) {

                JsonNode element = (JsonNode) nodeElements.next();
                String elementName = element.fieldNames().next();

                JsonNode instanceNode = element.elements().next();
                JsonNode runtimeConfigNode = instanceNode.get(YamlConfigDeserializer.YAML_INSTANCE);
                injectPropertiesByPrefix(runtimeConfigNode, props, elementName);
            }
        }
    }

    private static void injectPropertiesByPrefix(JsonNode propertiesAwareNode, Properties props, String prefix) {
        injectProperties(propertiesAwareNode, getFilteredByPrefix(props, prefix+"."), YamlConfigDeserializer.YAML_PROPERTIES);
    }

    private static Properties getFilteredByPrefix(Properties target, String prefix) {
        Properties result = new Properties();
        Enumeration<String> sysPropsNames = (Enumeration<String>) target.propertyNames();
        while (sysPropsNames.hasMoreElements()) {
            String key = sysPropsNames.nextElement();
            if (key.startsWith(prefix)) {
                result.setProperty(key.substring(prefix.length()), target.getProperty(key));
            }
        }
        return result.size()>0? result: null;

    }

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
        return getFilteredByPrefix(System.getProperties(), prefix+".");
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

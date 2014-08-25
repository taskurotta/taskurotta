package ru.taskurotta.bootstrap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created on 22.08.2014.
 */
public class PropertiesInjector {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesInjector.class);

    public static final String PROP_PREFIX = "ts.";

//    public static void injectProperties(JsonNode target, String location) {
//
//    }
//
//    public static void injectSystemProperties(Config target) {
//
//    }

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

//    public static JsonNode injectProperties(JsonNode target, Properties props) {
//        if (props!=null && props.size()>0) {
//            Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();
//            while (propNames.hasMoreElements()) {
//                String propName = propNames.nextElement();
//                if (isInjectable(propName)) {
//                    injectProperty(target, propName, props.getProperty(propName));
//                }
//            }
//        }
//        return target;
//    }
//
//    private static void injectProperty(JsonNode target, String propName, String propValue) {
//        String[] paths = propName.split("\\.");
//        for (int i = 1; i<paths.length; i++) {
//            String nodeName = paths[i];
//        }
//    }
//
//    private static boolean isInjectable(String name) {
//        return name!=null && name.startsWith(PROP_PREFIX);
//    }


}

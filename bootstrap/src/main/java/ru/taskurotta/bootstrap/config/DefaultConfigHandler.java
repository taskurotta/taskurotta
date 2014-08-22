package ru.taskurotta.bootstrap.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

/**
 * Created on 21.08.2014.
 */
public class DefaultConfigHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigHandler.class);

    public static final String PARAM_DEFAULT_CONFIG_LOCATION = "config.location";
    public static final String DEFAULT_CONFIG_NAME = "taskurotta/taskurotta.yml";
    public static final String TEST_CONFIG_NAME = "taskurotta/taskurotta-test.yml";

    public static Config getConfig(String arg) throws IOException {
        JsonNode resultCfg = parseDefaultCfg();

        if (arg != null) {
            File externalFile = new File(arg);
            if (!externalFile.exists()) {
                throw new IllegalArgumentException("External file does not exists ["+externalFile.getPath()+"]");
            }
            resultCfg = merge(resultCfg, Config.getYamlMapperInstance().readValue(externalFile, JsonNode.class));
        }

        if (resultCfg == null) {
            throw new IllegalArgumentException("Cannot parse config for arg ["+arg+"]");
        }

        logger.debug("try to parse config for yaml [{}]", resultCfg.toString());
        return Config.valueOf(resultCfg.toString());
    }

    public static JsonNode parseDefaultCfg() {
        JsonNode result = null;
        try {
            String cfgLocation = System.getProperty(PARAM_DEFAULT_CONFIG_LOCATION);
            if (cfgLocation != null) {
                File externalFile = new File(cfgLocation);
                if (externalFile.exists()) {
                    logger.debug("try to parse config from external file [{}]", cfgLocation);
                    result = Config.getYamlMapperInstance().readTree(externalFile);
                } else {
                    logger.debug("try to parse config from classpath file [{}]", cfgLocation);
                    URL resUrl = DefaultConfigHandler.class.getClassLoader().getResource(cfgLocation);
                    result = resUrl!=null? Config.getYamlMapperInstance().readTree(resUrl) : null;
                }
            } else {
                String source = TEST_CONFIG_NAME;
                InputStream configStream = DefaultConfigHandler.class.getClassLoader().getResourceAsStream(TEST_CONFIG_NAME);
                if (configStream == null) {
                    source = DEFAULT_CONFIG_NAME;
                    configStream = DefaultConfigHandler.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_NAME);
                }

                if (configStream!=null) {
                    logger.debug("try to parse default config from classpath file [{}]", source);
                    result = Config.getYamlMapperInstance().readTree(configStream);
                }
            }
        } catch(Exception e) {
            logger.error("Cannot parse default configuration", e);
        }

        return result;
    }

    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode != null && updateNode != null) {
            Iterator<String> fieldNames = updateNode.fieldNames();
            while (fieldNames.hasNext()) {

                String fieldName = fieldNames.next();
                JsonNode jsonNode = mainNode.get(fieldName);
                // if field exists and is an embedded object
                if (jsonNode != null && jsonNode.isObject()) {
                    merge(jsonNode, updateNode.get(fieldName));
                }
                else {
                    if (mainNode instanceof ObjectNode) {
                        // Overwrite field
                        JsonNode value = updateNode.get(fieldName);
                        ((ObjectNode) mainNode).put(fieldName, value);
                    }
                }

            }
        } else if (mainNode == null) {
            return updateNode;
        }

        return mainNode;
    }

    public static JsonNode addProperties(JsonNode mainNode, Properties runtimeProps, Properties spreaderProps) {



        return mainNode;
    }

}

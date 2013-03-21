package ru.taskurotta.bootstrap.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: romario
 * Date: 2/12/13
 * Time: 1:22 PM
 */
@JsonDeserialize(using = Config.ConfigDeserializer.class)
public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    public Map<String, RuntimeConfig> runtimeConfigs = new HashMap<String, RuntimeConfig>();
    public Map<String, SpreaderConfig> spreaderConfigs = new HashMap<String, SpreaderConfig>();
    public List<ActorConfig> actorConfigs = new LinkedList<ActorConfig>();

    public static Config valueOf(File configFile) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(configFile, Config.class);
    }


    public static Config valueOf(URL configURL) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(configURL, Config.class);
    }

    public static class ConfigDeserializer extends JsonDeserializer<Config> {

        public static final String YAML_RUNTIME = "runtime";
        public static final String YAML_INSTANCE = "instance";
        public static final String YAML_CLASS = "class";
        public static final String YAML_SPREADER = "spreader";
        public static final String YAML_ACTOR = "actor";

        @Override
        public Config deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {

            Config config = new Config();

            ObjectCodec oc = jsonParser.getCodec();

            JsonNode rootNode = null;
            try {
                rootNode = oc.readTree(jsonParser);
            } catch (IOException e) {
                throw new RuntimeException("Can not parse config", e);
            }

            JsonNode runtimesNode = rootNode.get(YAML_RUNTIME);

            if (runtimesNode != null) {

                for (Iterator runtimeElements = runtimesNode.elements(); runtimeElements.hasNext(); ) {

                    JsonNode runtimeElement = (JsonNode) runtimeElements.next();
                    logger.debug("runtimeElement [{}]", runtimeElement);

                    String runtimeConfigName = runtimeElement.fieldNames().next();
                    logger.debug("runtimeConfigName [{}]", runtimeConfigName);

                    JsonNode instanceDescriptionNode = runtimeElement.elements().next();
                    JsonNode runtimeConfigNode = (JsonNode) instanceDescriptionNode.get(YAML_INSTANCE);
                    logger.debug("runtimeConfigNode [{}]", runtimeConfigNode);


                    String runtimeConfigClassName = instanceDescriptionNode.get(YAML_CLASS).textValue();
                    logger.debug("runtimeConfigClassName [{}]", runtimeConfigClassName);

                    Class runtimeConfigClass = null;
                    try {
                        runtimeConfigClass = Class.forName(runtimeConfigClassName);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Can not find RuntimeConfig class: " + runtimeConfigClassName, e);
                    }

                    RuntimeConfig runtimeConfig = null;
                    try {

                        runtimeConfig = (RuntimeConfig) oc.treeToValue(runtimeConfigNode, runtimeConfigClass);
                    } catch (IOException e) {
                        throw new RuntimeException("Can not deserialize RuntimeConfig object: " + runtimeConfigClassName, e);
                    }

                    runtimeConfig.init();
                    config.runtimeConfigs.put(runtimeConfigName, runtimeConfig);
                }

            } else {
                logger.error("Not found RuntimeConfigs in configuration");
                throw new RuntimeException("Not found RuntimeConfigs in configuration file");
            }


            JsonNode spreadersNode = rootNode.get(YAML_SPREADER);

            if (spreadersNode != null) {
                for (Iterator spreaderElements = spreadersNode.elements(); spreaderElements.hasNext(); ) {

                    JsonNode spreaderElement = (JsonNode) spreaderElements.next();
                    logger.debug("spreaderElement [{}]", spreaderElement);

                    String spreaderConfigName = spreaderElement.fieldNames().next();
                    logger.debug("spreaderConfigName [{}]", spreaderConfigName);

                    JsonNode instanceDescriptionNode = spreaderElement.elements().next();
                    JsonNode spreaderConfigNode = (JsonNode) instanceDescriptionNode.get(YAML_INSTANCE);
                    logger.debug("spreaderConfigNode [{}]", spreaderConfigNode);


                    String spreaderConfigClassName = instanceDescriptionNode.get(YAML_CLASS).textValue();
                    logger.debug("spreaderConfigClassName [{}]", spreaderConfigClassName);

                    Class spreaderConfigClass = null;
                    try {
                        spreaderConfigClass = Class.forName(spreaderConfigClassName);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Can not find SpreaderConfig class: " + spreaderConfigClassName, e);
                    }

                    SpreaderConfig spreaderConfig = null;
                    try {
                        spreaderConfig = (SpreaderConfig) oc.treeToValue(spreaderConfigNode, spreaderConfigClass);
                    } catch (IOException e) {
                        throw new RuntimeException("Can not deserialize SpreaderConfig object: " + spreaderConfigClassName, e);
                    }

                    spreaderConfig.init();
                    config.spreaderConfigs.put(spreaderConfigName, spreaderConfig);

                }
            } else {
                logger.error("Not found TaskSpreaderConfigs in configuration");
                throw new RuntimeException("Not found TaskSpreaderConfigs in configuration file");
            }

            JsonNode actorsNode = rootNode.get(YAML_ACTOR);


            if (actorsNode != null) {

                Iterator<JsonNode> iterator = actorsNode.elements();
                while (iterator.hasNext()) {

                    JsonNode actorElement = (JsonNode) iterator.next();
                    logger.debug("actorElement [{}]", actorElement);

                    String actorConfigName = actorElement.fieldNames().next();
                    logger.debug("actorConfigName [{}]", actorConfigName);

                    JsonNode instanceDescriptionNode = actorElement.elements().next();

                    ActorConfig actorConfig = null;
                    try {
                        actorConfig = (ActorConfig) oc.treeToValue(instanceDescriptionNode, ActorConfig.class);
                    } catch (IOException e) {
                        throw new RuntimeException("Can not deserialize ActorConfig object.", e);
                    }


                    config.actorConfigs.add(actorConfig);
                }
            } else {
                logger.error("Not found Actors in configuration");
                throw new RuntimeException("Not found Actors in configuration file");
            }

            return config;
        }
    }
}

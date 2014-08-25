package ru.taskurotta.bootstrap.config.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.config.*;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created on 20.08.2014.
 */
public class YamlConfigDeserializer extends JsonDeserializer<Config> {
    private static final Logger logger = LoggerFactory.getLogger(YamlConfigDeserializer.class);

    public static final String YAML_RUNTIME = "runtime";
    public static final String YAML_INSTANCE = "instance";
    public static final String YAML_CLASS = "class";
    public static final String YAML_SPREADER = "spreader";
    public static final String YAML_ACTOR = "actor";
    public static final String YAML_RPOFILER = "profiler";
    public static final String YAML_POLICY = "policy";
    public static final String YAML_PROPERTIES_LOCATION = "propertiesLocation";
    public static final String YAML_PROPERTIES = "properties";

    @Override
    public Config deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {

        Config config = new Config();

        ObjectCodec oc = jsonParser.getCodec();

        JsonNode rootNode;
        try {
            rootNode = oc.readTree(jsonParser);
        } catch (IOException e) {
            throw new RuntimeException("Can not parse config", e);
        }

        JsonNode runtimesNode = rootNode.get(YAML_RUNTIME);
        if (runtimesNode != null) {
            parseRuntimeConfigs(runtimesNode, oc, config);
        } else {
            logger.error("Not found RuntimeConfigs in configuration");
            throw new RuntimeException("Not found RuntimeConfigs in configuration file");
        }

        JsonNode spreadersNode = rootNode.get(YAML_SPREADER);
        if (spreadersNode != null) {
            parseSpreaderConfigs(spreadersNode, oc, config);
        } else {
            logger.error("Not found TaskSpreaderConfigs in configuration");
            throw new RuntimeException("Not found TaskSpreaderConfigs in configuration file");
        }

        JsonNode profilersNode = rootNode.get(YAML_RPOFILER);
        if (profilersNode != null) {
            parseProfilerConfigs(profilersNode, oc, config);
        } else {
            logger.warn("Not found ProfilerConfigs in configuration");
        }

        JsonNode policiesNode = rootNode.get(YAML_POLICY);
        if (policiesNode != null) {
            parsePolicyConfig(policiesNode, oc, config);
        } else {
            logger.warn("Not found PolicyConfigs in configuration");
        }

        JsonNode actorsNode = rootNode.get(YAML_ACTOR);
        if (actorsNode != null) {
            parseActorConfigs(actorsNode, oc, config);
        } else {
            logger.error("Not found Actors in configuration");
            throw new RuntimeException("Not found Actors in configuration file");
        }

        return config;
    }

    private void parseRuntimeConfigs(JsonNode runtimesNode, ObjectCodec oc, Config config) {
        for (Iterator runtimeElements = runtimesNode.elements(); runtimeElements.hasNext(); ) {

            JsonNode runtimeElement = (JsonNode) runtimeElements.next();
            logger.debug("runtimeElement [{}]", runtimeElement);

            String runtimeConfigName = runtimeElement.fieldNames().next();
            logger.debug("runtimeConfigName [{}]", runtimeConfigName);

            JsonNode instanceDescriptionNode = runtimeElement.elements().next();
            JsonNode runtimeConfigNode = instanceDescriptionNode.get(YAML_INSTANCE);
            injectExternalProperties(runtimeConfigNode, runtimeConfigName);
            logger.debug("runtimeConfigNode [{}]", runtimeConfigNode);

            String runtimeConfigClassName = instanceDescriptionNode.get(YAML_CLASS).textValue();
            logger.debug("runtimeConfigClassName [{}]", runtimeConfigClassName);

            Class runtimeConfigClass;
            try {
                runtimeConfigClass = Class.forName(runtimeConfigClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Can not find RuntimeConfig class: " + runtimeConfigClassName, e);
            }

            RuntimeConfig runtimeConfig;
            try {
                runtimeConfig = (RuntimeConfig) oc.treeToValue(runtimeConfigNode, runtimeConfigClass);
            } catch (IOException e) {
                throw new RuntimeException("Can not deserialize RuntimeConfig object: " + runtimeConfigClassName, e);
            }

            runtimeConfig.init();
            config.runtimeConfigs.put(runtimeConfigName, runtimeConfig);
        }
    }

    private void parseSpreaderConfigs(JsonNode spreadersNode, ObjectCodec oc, Config config) {
        for (Iterator spreaderElements = spreadersNode.elements(); spreaderElements.hasNext(); ) {

            JsonNode spreaderElement = (JsonNode) spreaderElements.next();
            logger.debug("spreaderElement [{}]", spreaderElement);

            String spreaderConfigName = spreaderElement.fieldNames().next();
            logger.debug("spreaderConfigName [{}]", spreaderConfigName);

            JsonNode instanceDescriptionNode = spreaderElement.elements().next();
            JsonNode spreaderConfigNode = instanceDescriptionNode.get(YAML_INSTANCE);
            injectExternalProperties(spreaderConfigNode, spreaderConfigName);
            logger.debug("spreaderConfigNode [{}]", spreaderConfigNode);

            String spreaderConfigClassName = instanceDescriptionNode.get(YAML_CLASS).textValue();
            logger.debug("spreaderConfigClassName [{}]", spreaderConfigClassName);

            Class spreaderConfigClass;
            try {
                spreaderConfigClass = Class.forName(spreaderConfigClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Can not find SpreaderConfig class: " + spreaderConfigClassName, e);
            }

            SpreaderConfig spreaderConfig;
            try {
                spreaderConfig = (SpreaderConfig) oc.treeToValue(spreaderConfigNode, spreaderConfigClass);
            } catch (IOException e) {
                throw new RuntimeException("Can not deserialize SpreaderConfig object: " + spreaderConfigClassName, e);
            }

            spreaderConfig.init();
            config.spreaderConfigs.put(spreaderConfigName, spreaderConfig);
        }
    }

    private void parseProfilerConfigs(JsonNode profilersNode, ObjectCodec oc, Config config) {
        for (Iterator profilerElements = profilersNode.elements(); profilerElements.hasNext(); ) {

            JsonNode profilerElement = (JsonNode) profilerElements.next();
            logger.debug("profilerElement [{}]", profilerElement);

            String profilerConfigName = profilerElement.fieldNames().next();
            logger.debug("profilerConfigName [{}]", profilerConfigName);

            JsonNode instanceDescriptionNode = profilerElement.elements().next();
            JsonNode profilerConfigNode = instanceDescriptionNode.get(YAML_INSTANCE);
            injectExternalProperties(profilerConfigNode, profilerConfigName);
            logger.debug("profilerConfigNode [{}]", profilerConfigNode);

            String profilerConfigClassName = instanceDescriptionNode.get(YAML_CLASS).textValue();
            logger.debug("profilerConfigClassName [{}]", profilerConfigClassName);

            Class profilerConfigClass;
            try {
                profilerConfigClass = Class.forName(profilerConfigClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Can not find ProfilerConfig class: " + profilerConfigClassName, e);
            }

            ProfilerConfig profilerConfig;
            try {
                profilerConfig = (ProfilerConfig) oc.treeToValue(profilerConfigNode, profilerConfigClass);
            } catch (IOException e) {
                throw new RuntimeException("Can not deserialize ProfilerConfig object: " + profilerConfigClassName, e);
            }

            config.profilerConfigs.put(profilerConfigName, profilerConfig);
        }
    }

    private void parsePolicyConfig(JsonNode policyNodes, ObjectCodec oc, Config config) {
        for (Iterator policyElements = policyNodes.elements(); policyElements.hasNext(); ) {

            JsonNode policyElement = (JsonNode) policyElements.next();
            logger.debug("policyElement [{}]", policyElement);

            String policyConfigName = policyElement.fieldNames().next();
            logger.debug("policyConfigName [{}]", policyConfigName);

            JsonNode instanceDescriptionNode = policyElement.elements().next();
            JsonNode policyConfigNode = instanceDescriptionNode.get(YAML_INSTANCE);
            injectExternalProperties(policyConfigNode, policyConfigName);
            logger.debug("policyConfigNode [{}]", policyConfigNode);

            String policyConfigClassName = instanceDescriptionNode.get(YAML_CLASS).textValue();
            logger.debug("policyConfigClassName [{}]", policyConfigClassName);

            Class policyConfigClass;
            try {
                policyConfigClass = Class.forName(policyConfigClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Can not find RetryPolicyConfig class: " + policyConfigClassName, e);
            }

            RetryPolicyFactory retryPolicyFactory;
            try {
                retryPolicyFactory = (RetryPolicyFactory) oc.treeToValue(policyConfigNode, policyConfigClass);
            } catch (IOException e) {
                throw new RuntimeException("Can not deserialize RetryPolicyConfig object: " + policyConfigClassName, e);
            }

            config.policyConfigs.put(policyConfigName, retryPolicyFactory);
        }
    }

    private void parseActorConfigs(JsonNode actorsNode, ObjectCodec oc, Config config) {
        for (Iterator actorElements = actorsNode.elements(); actorElements.hasNext(); ) {

            JsonNode actorElement = (JsonNode) actorElements.next();
            logger.debug("actorElement [{}]", actorElement);

            String actorConfigName = actorElement.fieldNames().next();
            logger.debug("actorConfigName [{}]", actorConfigName);

            JsonNode instanceDescriptionNode = actorElement.elements().next();
            injectExternalProperties(instanceDescriptionNode, actorConfigName);
            ActorConfig actorConfig;
            try {
                actorConfig = oc.treeToValue(instanceDescriptionNode, ActorConfig.class);

                if (actorConfig.getRuntimeConfig() == null) {
                    if (config.runtimeConfigs.size() == 1) {
                        String runtimeConfigName = config.runtimeConfigs.keySet().iterator().next();
                        actorConfig.setRuntimeConfig(runtimeConfigName);
                    } else {
                        throw new RuntimeException("Don't set RuntimeConfig for [" + actorConfigName + "] or exists few RuntimeConfig");
                    }
                }

                if (actorConfig.getSpreaderConfig() == null) {
                    if (config.spreaderConfigs.size() == 1) {
                        String spreaderConfigName = config.spreaderConfigs.keySet().iterator().next();
                        actorConfig.setSpreaderConfig(spreaderConfigName);
                    } else {
                        throw new RuntimeException("Don't set SpreaderConfig for [" + actorConfigName + "] or exists few SpreaderConfig");
                    }
                }

                if (actorConfig.getProfilerConfig() == null) {
                    if (config.profilerConfigs.size() == 1) {
                        String profilerConfigName = config.profilerConfigs.keySet().iterator().next();
                        actorConfig.setProfilerConfig(profilerConfigName);
                    }
                }

                if (actorConfig.getPolicyConfig() == null) {
                    if (config.policyConfigs.size() == 1) {
                        String policyConfigName = config.policyConfigs.keySet().iterator().next();
                        actorConfig.setPolicyConfig(policyConfigName);
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException("Can not deserialize ActorConfig object.", e);
            }

            config.actorConfigs.add(actorConfig);
        }
    }

    private static void injectExternalProperties(JsonNode propertiesAwareNode, String prefix) {
        if (propertiesAwareNode!=null) {
            if (propertiesAwareNode.hasNonNull(YAML_PROPERTIES_LOCATION)) {
                String[] locations = propertiesAwareNode.get(YAML_PROPERTIES_LOCATION).textValue().split("\\s*\\,\\s*");
                for (String location : locations) {
                    PropertiesInjector.injectProperties(propertiesAwareNode, location, YAML_PROPERTIES);
                }
                ((ObjectNode) propertiesAwareNode).remove(YAML_PROPERTIES_LOCATION);
            }
            PropertiesInjector.injectSystemProperties(propertiesAwareNode, prefix, YAML_PROPERTIES);
        }
    }

}

package ru.taskurotta.bootstrap.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	public Map<String, ProfilerConfig> profilerConfigs = new HashMap<String, ProfilerConfig>();
	public Map<String, LoggingConfig> loggingConfigs = new HashMap<String, LoggingConfig>();
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
		public static final String YAML_RPOFILER = "profiler";
		public static final String YAML_LOGGING = "logging";

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

			JsonNode loggingsNode = rootNode.get(YAML_LOGGING);
			if (loggingsNode == null) {
				logger.warn("Not found LoggingConfig in configuration");
			} else {
				parseLoggingConfig(loggingsNode, oc, config);
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

		private void parseLoggingConfig(JsonNode loggingsNode, ObjectCodec oc, Config config) {
			for (Iterator loggingElements = loggingsNode.elements(); loggingElements.hasNext(); ) {

				JsonNode loggingElement = (JsonNode) loggingElements.next();
				logger.debug("loggingElement [{}]", loggingElement);

				String loggingConfigName = loggingElement.fieldNames().next();
				logger.debug("loggingConfigName [{}]", loggingConfigName);

				JsonNode instanceDescriptionNode = loggingElement.elements().next();
				JsonNode loggingConfigNode = instanceDescriptionNode.get(YAML_INSTANCE);
				logger.debug("loggingConfigNode [{}]", loggingConfigNode);

				String loggingConfigClassName = instanceDescriptionNode.get(YAML_CLASS).textValue();
				logger.debug("loggingConfigClassName [{}]", loggingConfigClassName);

				Class loggingConfigClass;
				try {
					loggingConfigClass = Class.forName(loggingConfigClassName);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Can not find LoggingConfig class: " + loggingConfigClassName, e);
				}

				LoggingConfig loggingConfig;
				try {
					loggingConfig = (LoggingConfig) oc.treeToValue(loggingConfigNode, loggingConfigClass);
				} catch (IOException e) {
					throw new RuntimeException("Can not deserialize LoggingConfig object: " + loggingConfigClassName, e);
				}

				config.loggingConfigs.put(loggingConfigName, loggingConfig);
			}
		}

		private void parseActorConfigs(JsonNode actorsNode, ObjectCodec oc, Config config) {
			for (Iterator actorElements = actorsNode.elements(); actorElements.hasNext(); ) {

				JsonNode actorElement = (JsonNode) actorElements.next();
				logger.debug("actorElement [{}]", actorElement);

				String actorConfigName = actorElement.fieldNames().next();
				logger.debug("actorConfigName [{}]", actorConfigName);

				JsonNode instanceDescriptionNode = actorElement.elements().next();

				ActorConfig actorConfig;
				try {
					actorConfig = oc.treeToValue(instanceDescriptionNode, ActorConfig.class);
				} catch (IOException e) {
					throw new RuntimeException("Can not deserialize ActorConfig object.", e);
				}

				config.actorConfigs.add(actorConfig);
			}
		}
	}
}

package ru.taskurotta.bootstrap.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: stukushin
 * Date: 28.03.13
 * Time: 14:44
 */
@JsonDeserialize(using = LoggingConfig.LoggingConfigDeserializer.class)
public class LoggingConfig {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfig.class);

    private static File configFile = null;

    public static class LoggingConfigDeserializer extends JsonDeserializer<LoggingConfig> {

        public static final String YAML_LOGBACK = "logback";
        public static final String YAML_LOGBACK_APPENDER = "appender";
        public static final String YAML_LOGBACK_LOGGER = "logger";
        public static final String YAML_LOGBACK_ROOT = "root";
        public static final String YAML_LOGBACK_APPENDER_REF = "appender-ref";

        @Override
        public LoggingConfig deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            LoggingConfig loggingConfig = new LoggingConfig();
            ObjectCodec objectCodec = jsonParser.getCodec();

            Element configuration = new Element("configuration");
            Document document = new Document(configuration);
            document.setRootElement(configuration);

            JsonNode logbackNode;
            try {
                logbackNode = ((JsonNode) objectCodec.readTree(jsonParser)).get(YAML_LOGBACK);
            } catch (IOException e) {
                throw new RuntimeException("Can not parse config", e);
            }

            if (logbackNode == null) {
                return null;
            }

            JsonNode appendersNode = logbackNode.get(YAML_LOGBACK_APPENDER);
            if (appendersNode != null) {
                List<Element> appenderElements = parseAppenders(appendersNode);

                if (appenderElements == null || appenderElements.isEmpty()) {
                    logger.error("Not found appenders config in configuration");
                    throw new RuntimeException("Not found appenders config in configuration file");
                }

                configuration.addContent(appenderElements);
            } else {
                logger.error("Not found appender node in configuration");
                throw new RuntimeException("Not found appender node in configuration file");
            }

            JsonNode loggersNode = logbackNode.get(YAML_LOGBACK_LOGGER);
            if (loggersNode == null) {
                logger.error("Not found logger node in configuration");
                throw new RuntimeException("Not found logger node in configuration file");
            } else {
                List<Element> loggerElements = parseLoggers(loggersNode);

                if (loggerElements == null || loggerElements.isEmpty()) {
                    logger.error("Not found loggers config in configuration");
                    throw new RuntimeException("Not found loggers config in configuration file");
                }

                configuration.addContent(loggerElements);
            }

            JsonNode rootNode = logbackNode.get(YAML_LOGBACK_ROOT);
            if (rootNode == null) {
                logger.error("Not found root node in configuration");
                throw new RuntimeException("Not root logger node in configuration file");
            } else {
                JsonNode appenderRefsNode = rootNode.get(YAML_LOGBACK_APPENDER_REF);

                if (appenderRefsNode == null) {
                    logger.error("Not found appender-ref node in configuration");
                    throw new RuntimeException("Not found appender-ref node in configuration");
                } else {
                    List<Element> appenderRefElements = parseAppenderRefs(appenderRefsNode);

                    if (appenderRefElements == null || appenderRefElements.isEmpty()) {
                        logger.error("Not found appender-ref config in configuration");
                        throw new RuntimeException("Not found appender-ref config in configuration file");
                    }

                    Element root = new Element("root");
                    root.addContent(appenderRefElements);
                    configuration.addContent(root);
                }
            }

            saveXml(document);

            return loggingConfig;
        }

        private List<Element> parseAppenders(JsonNode appendersNode) {
            List<Element> appenders = new ArrayList<Element>();

            for (JsonNode appenderNode : appendersNode) {
                String appenderName = appenderNode.fieldNames().next();
                Element appenderElement = new Element("appender");
                appenders.add(appenderElement);

                JsonNode content = appenderNode.elements().next();
                parseNode(content, appenderElement, appenderName);
            }

            return appenders;
        }

        private List<Element> parseLoggers(JsonNode loggersNode) {
            List<Element> loggers = new ArrayList<Element>();

            for (JsonNode loggerNode : loggersNode) {
                String loggerName = loggerNode.fieldNames().next();
                Element loggerElement = new Element("logger");
                loggers.add(loggerElement);

                JsonNode content = loggerNode.elements().next();
                parseNode(content, loggerElement, loggerName);
            }

            return loggers;
        }

        private List<Element> parseAppenderRefs(JsonNode appenderRefsNode) {
            List<Element> appenderRefs = new ArrayList<Element>();

            for (JsonNode appenderRefNode : appenderRefsNode) {
                String loggerName = appenderRefNode.fieldNames().next();
                Element appenderRefElement = new Element("appender-ref");
                appenderRefs.add(appenderRefElement);

                JsonNode content = appenderRefNode.elements().next();
                parseNode(content, appenderRefElement, loggerName);
            }

            return appenderRefs;
        }

        private Element createSimpleElement(String name, String value) {
            return new Element(name).addContent(value);
        }

        private Element parseNode(JsonNode node, Element element, String elementName) {
            Iterator<String> namesIterator = node.fieldNames();
            while (namesIterator.hasNext()) {
                String name = namesIterator.next();

                if (name.equals("key")) {
                    element.setAttribute(node.get(name).textValue().replaceAll("\\\\@", ""), elementName);
                } else if (name.startsWith("\\@")) {
                    element.setAttribute(name.replaceAll("\\\\@", ""), node.get(name).textValue());
                } else {
                    if (node.get(name).elements().hasNext() || node.get(name).fieldNames().hasNext()) {
                        element.addContent(parseNode(node.get(name), new Element(name), ""));
                    } else {
                        String value = node.get(name).textValue();
                        if (StringUtils.hasText(value) && value.contains("\\%")) {
                            value = value.replaceAll("\\\\%", "%");
                        }
                        element.addContent(createSimpleElement(name, value));
                    }
                }
            }

            return element;
        }

        private void saveXml(Document document) {
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());

            try {
                configFile = File.createTempFile("logback-user-config", ".xml");
                xmlOutput.output(document, new FileWriter(configFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public File getConfigFile() {
        return configFile;
    }
}

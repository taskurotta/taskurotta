package ru.taskurotta.bootstrap.config.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdom.Document;
import org.jdom.Element;
import ru.taskurotta.bootstrap.config.serialization.LoggerConfigDeserializer;

import java.io.IOException;
import java.util.Map;

/**
 * Created on 22.08.2014.
 */
public class SimpleLogConverter {

    public static Document convert(JsonNode node) throws IOException {
        SimpleLogConfig slCfg = new ObjectMapper().readValue(node.toString(), SimpleLogConfig.class);
        return convert(slCfg);
    }

    public static Document convert(SimpleLogConfig cfg) {
        Element configuration = new Element("configuration");
        Document document = new Document(configuration);
        document.setRootElement(configuration);

        Element root = new Element("root");
        root.setAttribute("level", cfg.getLevel());

        Element consoleAppender = getConsoleAppender(cfg.getConsole(), "STDOUT");
        if (consoleAppender != null) {
            configuration.addContent(consoleAppender);
            root.addContent(getAppenderRef("STDOUT"));
        }

        Element fileAppender = getFileAppender(cfg.getFile(), "FILE");
        if (fileAppender != null) {
            configuration.addContent(fileAppender);
            root.addContent(getAppenderRef("FILE"));
        }

        Map<String, String> loggers = cfg.getLoggers();
        if (loggers!=null && !loggers.isEmpty()) {
            for (Map.Entry<String, String> loggerEntry: loggers.entrySet()) {
                Element logger = new Element("logger");
                logger.setAttribute("name", loggerEntry.getKey());
                logger.setAttribute("level", loggerEntry.getValue());
                configuration.addContent(logger);
            }
        }

        configuration.addContent(root);
        return document;
    }


    public static Element getAppenderRef(String name) {
        Element appenderRef = new Element("appender-ref");
        appenderRef.setAttribute("ref", name);
        return appenderRef;
    }

    public static Element getConsoleAppender(ConsoleAppenderCfg cfg, String name) {
        Element result = null;
        if (cfg !=null && cfg.isEnabled()) {
            result = new Element("appender");
            result.setAttribute("class", "ch.qos.logback.core.ConsoleAppender");
            result.setAttribute("name", name);
            result.addContent(getEncoderElement(cfg.getLogFormat()));
        }
        return result;
    }

    public static Element getFileAppender(FileAppenderCfg cfg, String name) {
        Element result = null;
        if (cfg!=null && cfg.isEnabled()) {
            result = new Element(LoggerConfigDeserializer.YAML_LOGBACK_APPENDER);
            result.setAttribute("name", name);
            result.setAttribute("class", "ch.qos.logback.core.rolling.RollingFileAppender");

            Element file = new Element("file");
            file.setText(cfg.getCurrentLogFilename());

            result.addContent(file);
            result.addContent(getRollingPolicy(cfg.getArchivedLogFilenamePattern()));
            result.addContent(getEncoderElement(cfg.getLogFormat()));

        }
        return result;
    }

    public static Element getRollingPolicy(String archievedFileNamePattern) {
        Element rollingPolicy = new Element("rollingPolicy");
        rollingPolicy.setAttribute("class", "ch.qos.logback.core.rolling.TimeBasedRollingPolicy");
        Element fileNamePattern = new Element("fileNamePattern");
        fileNamePattern.setText(archievedFileNamePattern);
        rollingPolicy.addContent(fileNamePattern);
        return rollingPolicy;
    }

    public static Element getEncoderElement(String pattern) {
        Element encoderElem = new Element("encoder");
        Element patternElem = new Element("pattern");
        patternElem.setText(pattern);
        encoderElem.setContent(patternElem);
        return encoderElem;
    }


}

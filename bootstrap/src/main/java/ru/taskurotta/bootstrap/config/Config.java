package ru.taskurotta.bootstrap.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.config.serialization.LoggerConfigDeserializer;
import ru.taskurotta.bootstrap.config.serialization.YamlConfigDeserializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Date: 2/12/13 1:22 PM
 */
public class Config {

    public Map<String, RuntimeConfig> runtimeConfigs = new HashMap<String, RuntimeConfig>();
    public Map<String, SpreaderConfig> spreaderConfigs = new HashMap<String, SpreaderConfig>();
    public Map<String, ProfilerConfig> profilerConfigs = new HashMap<String, ProfilerConfig>();
    public Map<String, RetryPolicyFactory> policyConfigs = new HashMap<String, RetryPolicyFactory>();
    public List<ActorConfig> actorConfigs = new LinkedList<ActorConfig>();

    public static ObjectMapper getYamlMapperInstance() {
        SimpleModule serializationModule = new SimpleModule();
        serializationModule.addDeserializer(Config.class, new YamlConfigDeserializer());
        serializationModule.addDeserializer(Document.class, new LoggerConfigDeserializer());

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(serializationModule);
        return mapper;
    }

    public static Config valueOf(String content) throws IOException {
        ObjectMapper mapper = getYamlMapperInstance();
        initLogging(content, mapper);
        return mapper.readValue(content, Config.class);
    }

    public static Config valueOf(File configFile) throws IOException {
        ObjectMapper mapper = getYamlMapperInstance();
        initLogging(configFile.toURI().toURL(), mapper);
        return mapper.readValue(configFile, Config.class);
    }

    public static Config valueOf(URL configURL) throws IOException {
        ObjectMapper mapper = getYamlMapperInstance();
        initLogging(configURL, mapper);
        return mapper.readValue(configURL, Config.class);
    }

    public static void initLogging(String configContent, ObjectMapper mapper) throws IOException {
        if (System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY) == null) {//have no external logback.xml location specified
            initLogback(mapper.readValue(configContent, Document.class));
        }
    }

    public static void initLogging(URL configURL, ObjectMapper mapper) throws IOException {
        if (System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY) == null) {//have no external logback.xml location specified
            initLogback(mapper.readValue(configURL, Document.class));
        }
    }

    private static void initLogback(Document yamlLogbackDoc) throws IOException {
        if (yamlLogbackDoc != null) {//has logger config in yaml
            // assume SLF4J is bound to logback in the current environment
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                InputStream is = new ByteArrayInputStream(new XMLOutputter().outputString(yamlLogbackDoc).getBytes("UTF-8"));
                // Call context.reset() to clear any previous configuration, e.g. default
                // configuration. For multi-step configuration, omit calling context.reset().
                context.reset();
                configurator.doConfigure(is);
            } catch (JoranException je) {
                // StatusPrinter will handle this
            }
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        }
    }

}

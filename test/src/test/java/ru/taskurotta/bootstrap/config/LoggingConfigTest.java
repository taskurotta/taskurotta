package ru.taskurotta.bootstrap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 19:03
 */
public class LoggingConfigTest {

    @Test
    public void testGetConfigFile() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        File configFile = new File(Thread.currentThread().getContextClassLoader().getResource("test-conf.yml").getFile());
        YamlConfigFactory.valueOf(configFile);

        LoggingConfig loggingConfig = mapper.readValue(configFile, LoggingConfig.class);
        File logbackFile = loggingConfig.getConfigFile();

        assertTrue(logbackFile.exists());

        SAXBuilder builder = new SAXBuilder();
        Document logbackDocument = builder.build(logbackFile);

        File logbackOrigFile = new File(Thread.currentThread().getContextClassLoader().getResource("logback.xml").getFile());
        Document logbackOrigDocument = builder.build(logbackOrigFile);

        assertEquals(logbackOrigDocument.getRootElement().getChildren("appender").size(), logbackDocument.getRootElement().getChildren("appender").size());
    }
}

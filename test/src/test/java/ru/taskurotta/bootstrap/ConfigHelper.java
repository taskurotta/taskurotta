package ru.taskurotta.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdom.output.DOMOutputter;
import org.w3c.dom.Document;
import ru.taskurotta.bootstrap.config.Config;
import ru.taskurotta.bootstrap.config.logging.SimpleLogConfig;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Created on 22.08.2014.
 */
public class ConfigHelper {

    public static SimpleLogConfig getYamlLogConfig(String name) throws Exception {
        ObjectMapper mapper = Config.getYamlMapperInstance();
        File configFile = new File(Thread.currentThread().getContextClassLoader().getResource(name).getFile());
        return mapper.readValue(configFile, SimpleLogConfig.class);
    }

    public static Document getYamlConfiguration(String name) throws Exception {
        ObjectMapper mapper = Config.getYamlMapperInstance();
        File configFile = new File(Thread.currentThread().getContextClassLoader().getResource(name).getFile());
        Config.valueOf(configFile);

        org.jdom.Document loggingCfgDoc = mapper.readValue(configFile, org.jdom.Document.class);
        DOMOutputter outPutter = new DOMOutputter();
        Document yamlDoc = outPutter.output(loggingCfgDoc);
        yamlDoc.normalizeDocument();
        yamlDoc.normalize();

        return yamlDoc;
    }

    public static Document getXmlConfiguartion(String name) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document xmlDoc = db.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
        xmlDoc.normalizeDocument();
        xmlDoc.normalize();
        return xmlDoc;
    }

    public static Document getXmlConfiguartion(byte[] content) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document xmlDoc = db.parse(new ByteArrayInputStream(content));
        xmlDoc.normalizeDocument();
        xmlDoc.normalize();
        return xmlDoc;
    }

}

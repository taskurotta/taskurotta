package ru.taskurotta.bootstrap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.jdom.output.DOMOutputter;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 19:03
 */
public class LoggingConfigTest {

    @Before
    public void setUp(){
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
    }

    @Test
    public void testParseConfigFile() throws Exception {

        Document docFromYaml = getYamlConfiguration("test-conf.yml");
        Document docFromOriginalXml = getXmlConfiguartion("test-conf-logback.xml");

        XMLAssert.assertXMLEqual(docFromOriginalXml, docFromYaml);

        Document docFromSpoiledXml = getXmlConfiguartion("test-conf-logback-spoiled.xml");
        XMLAssert.assertXMLNotEqual(docFromSpoiledXml, docFromYaml);

    }


    private Document getYamlConfiguration(String name) throws Exception {
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

    private Document getXmlConfiguartion(String name) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document xmlDoc = db.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(name));
        xmlDoc.normalizeDocument();
        xmlDoc.normalize();
        return xmlDoc;
    }

}

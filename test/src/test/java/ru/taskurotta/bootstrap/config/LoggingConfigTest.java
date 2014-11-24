package ru.taskurotta.bootstrap.config;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.taskurotta.bootstrap.ConfigHelper;

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

        Document docFromYaml = ConfigHelper.getYamlConfiguration("test-conf.yml");
        Document docFromOriginalXml = ConfigHelper.getXmlConfiguartion("test-conf-logback.xml");

        XMLAssert.assertXMLEqual(docFromOriginalXml, docFromYaml);

        Document docFromSpoiledXml = ConfigHelper.getXmlConfiguartion("test-conf-logback-spoiled.xml");
        XMLAssert.assertXMLNotEqual(docFromSpoiledXml, docFromYaml);

    }

}

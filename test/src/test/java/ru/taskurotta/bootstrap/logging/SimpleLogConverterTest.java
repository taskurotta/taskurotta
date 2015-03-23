package ru.taskurotta.bootstrap.logging;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.bootstrap.ConfigHelper;
import ru.taskurotta.bootstrap.config.logging.SimpleLogConfig;
import ru.taskurotta.bootstrap.config.logging.SimpleLogConverter;

/**
 * Created on 22.08.2014.
 */
public class SimpleLogConverterTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleLogConverterTest.class);

    @Before
    public void setUp(){
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
    }

    @Test
    public void testConvert() throws Exception {
        org.w3c.dom.Document defaultShouldBe = ConfigHelper.getXmlConfiguartion("taskurotta/logging/default.xml");
        org.w3c.dom.Document defaultConverted = asW3cDocument(SimpleLogConfig.defaultConfiguration());
        XMLAssert.assertXMLEqual(defaultShouldBe, defaultConverted);

        org.w3c.dom.Document onlyConsoleShouldBe = ConfigHelper.getXmlConfiguartion("taskurotta/logging/console-only.xml");

        SimpleLogConfig consoleCfg1 = SimpleLogConfig.defaultConfiguration();
        consoleCfg1.setFile(null);
        org.w3c.dom.Document onlyConsoleConverted1 = asW3cDocument(consoleCfg1);
        XMLAssert.assertXMLEqual(onlyConsoleShouldBe, onlyConsoleConverted1);

        SimpleLogConfig consoleCfg2 = SimpleLogConfig.defaultConfiguration();
        consoleCfg2.getFile().setEnabled(false);
        org.w3c.dom.Document onlyConsoleConverted2 = asW3cDocument(consoleCfg2);
        XMLAssert.assertXMLEqual(onlyConsoleShouldBe, onlyConsoleConverted2);

        org.w3c.dom.Document onlyFileShouldBe = ConfigHelper.getXmlConfiguartion("taskurotta/logging/file-only.xml");

        SimpleLogConfig fileCfg1 = SimpleLogConfig.defaultConfiguration();
        fileCfg1.setConsole(null);
        org.w3c.dom.Document onlyFileConverted1 = asW3cDocument(fileCfg1);
        XMLAssert.assertXMLEqual(onlyFileShouldBe, onlyFileConverted1);

        SimpleLogConfig fileCfg2 = SimpleLogConfig.defaultConfiguration();
        fileCfg2.getConsole().setEnabled(false);
        org.w3c.dom.Document onlyFileConverted2 = asW3cDocument(fileCfg2);
        XMLAssert.assertXMLEqual(onlyFileShouldBe, onlyFileConverted2);

    }


    private org.w3c.dom.Document asW3cDocument(SimpleLogConfig cfg) throws Exception {
        Document doc = SimpleLogConverter.convert(cfg);
        XMLOutputter output = new XMLOutputter();
        String result = output.outputString(doc);
        logger.debug("Result logback xml is [{}]", result);
        return ConfigHelper.getXmlConfiguartion(result.getBytes("UTF-8"));
    }

}

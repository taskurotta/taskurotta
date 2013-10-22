package ru.taskurotta.spring.configs;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 13:44
 */
public class RuntimeConfigPathXmlApplicationContextTest {

    private RuntimeConfigPathXmlApplicationContext runtimeConfigPathXmlApplicationContext;

    private String testProperty = "TEST";

    @Before
    public void setUp() throws Exception {
        String pathToXmlContext = "/RuntimeBeans.xml";

        Properties properties = new Properties();
        properties.setProperty("testProperty", testProperty);

        runtimeConfigPathXmlApplicationContext = new RuntimeConfigPathXmlApplicationContext();
        runtimeConfigPathXmlApplicationContext.setContext(pathToXmlContext);
        runtimeConfigPathXmlApplicationContext.setProperties(properties);
        runtimeConfigPathXmlApplicationContext.init();

    }

    @Test
    public void testGetRuntimeProcessor() throws Exception {
        assertNotNull(runtimeConfigPathXmlApplicationContext.getRuntimeProcessor(TestActor.class));
    }

    @Test
    public void testProperties() {

        TestActorImpl testActor = (TestActorImpl) runtimeConfigPathXmlApplicationContext.getApplicationContext().getBean("testActor");

        assertEquals(testProperty, testActor.getTestProperty());
    }
}

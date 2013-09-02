package ru.taskurotta.spring.configs;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * User: stukushin
 * Date: 01.04.13
 * Time: 13:44
 */
public class RuntimeConfigPathXmlApplicationContextTest {

    private RuntimeConfigPathXmlApplicationContext runtimeConfigPathXmlApplicationContext;

    @Before
    public void setUp() throws Exception {
        String pathToXmlContext = "/RuntimeBeans.xml";

        runtimeConfigPathXmlApplicationContext = new RuntimeConfigPathXmlApplicationContext();
        runtimeConfigPathXmlApplicationContext.setContext(pathToXmlContext);
        runtimeConfigPathXmlApplicationContext.init();

    }

    @Test
    public void testGetRuntimeProcessor() throws Exception {
        assertNotNull(runtimeConfigPathXmlApplicationContext.getRuntimeProcessor(TestActor.class));
    }
}

package ru.taskurotta.spring.configs;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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
    @Ignore
    public void testGetRuntimeProcessor() throws Exception {
        assertNotNull(runtimeConfigPathXmlApplicationContext.getRuntimeProcessor(TestActor.class));
    }
}
